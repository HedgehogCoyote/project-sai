import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/** Standalone PostgreSQL experiment. No application query or persistent data is changed. */
public class SearchBenchmark {
    private static final int WARMUP = 5, RUNS = 30;
    private static final String FILTER = " FROM search_bench_users WHERE upper(login_id) LIKE upper(?) ESCAPE '\\' ORDER BY login_id FETCH FIRST 10 ROWS ONLY";
    private static final String FULL = "SELECT id, name, login_id, password_hash, created_at, phone_number, email" + FILTER;
    private static final String PROJECTED = "SELECT id, login_id, name" + FILTER;
    private static final Map<String, String> CASES = new LinkedHashMap<>();
    static {
        CASES.put("two_chars", "ab"); CASES.put("three_chars", "a7f");
        CASES.put("rare_long", "rare00001000"); CASES.put("common", "user");
        CASES.put("no_match", "zznomatch"); CASES.put("mixed_case", "miXed");
        CASES.put("literal_percent", "%user"); CASES.put("literal_underscore", "_user");
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length > 0 ? args[0] : "../docs/benchmarks/user-search");
        boolean customOnly = args.length > 1 && args[1].equals("--custom-only");
        Files.createDirectories(output.resolve("plans"));
        String url = System.getenv().getOrDefault("BENCH_DB_URL", "jdbc:postgresql://localhost:5432/sai");
        String username = System.getenv().getOrDefault("BENCH_DB_USER", "postgres");
        String password = Objects.requireNonNull(System.getenv("DB_PASSWORD"), "DB_PASSWORD is required");
        String schema = "search_bench_" + UUID.randomUUID().toString().replace("-", "");
        List<String> rows = new ArrayList<>();
        rows.add("users,scenario,variant,columns,result_count,p50_ms,p95_ms,samples_ms");
        List<String> metadata = new ArrayList<>();
        if (customOnly) {
            rows = new ArrayList<>(Files.readAllLines(output.resolve("results.csv")));
            if (rows.size() != 73) throw new IllegalStateException("Complete the default-plan measurement before --custom-only");
            metadata.addAll(Files.readAllLines(output.resolve("environment.txt")));
            metadata.add("Additional diagnostic phase: force_custom_plan; prior default-plan measurements preserved.");
        }
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                metadata.add("PostgreSQL: " + connection.getMetaData().getDatabaseProductVersion());
                metadata.add("Java: " + System.getProperty("java.version"));
                metadata.add("Started at UTC: " + java.time.Instant.now());
                metadata.add("OS: " + System.getProperty("os.name") + "; processors=" + Runtime.getRuntime().availableProcessors());
                metadata.add("Warmup: " + WARMUP + "; samples per case: " + RUNS);
                metadata.add("Timing: sequential JDBC execute + result consumption; not HTTP/JPA API latency.");
                metadata.add("Temporary table, warm cache, synthetic distribution; not production concurrency or cold-cache measurements.");
                execute(connection, "SET LOCAL statement_timeout = '30s'");
                execute(connection, "CREATE SCHEMA " + schema);
                String extensionSchema;
                try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(
                        "SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace WHERE e.extname = 'pg_trgm'")) {
                    extensionSchema = rs.next() ? rs.getString(1) : null;
                }
                if (extensionSchema == null) {
                    execute(connection, "CREATE EXTENSION pg_trgm WITH SCHEMA " + schema);
                    extensionSchema = schema;
                }
                execute(connection, "SET LOCAL search_path TO " + schema + ", \"" + extensionSchema.replace("\"", "\"\"") + "\", public");
                execute(connection, """
                        CREATE TEMP TABLE search_bench_users (
                          id BIGINT PRIMARY KEY, name VARCHAR(50) NOT NULL,
                          login_id VARCHAR(50) NOT NULL UNIQUE,
                          password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL,
                          phone_number VARCHAR(20), email VARCHAR(255)
                        ) ON COMMIT DROP
                        """);
                for (int count : new int[]{10_000, 100_000, 1_000_000}) {
                    System.out.println("Dataset " + count + ": creating synthetic users");
                    execute(connection, "DROP INDEX IF EXISTS search_bench_trgm");
                    execute(connection, "TRUNCATE search_bench_users");
                    execute(connection, """
                            INSERT INTO search_bench_users
                            SELECT i, 'Synthetic ' || i,
                              CASE WHEN i = 1 THEN 'literal%user'
                                   WHEN i = 2 THEN 'literal_user'
                                   WHEN i = 3 THEN 'MiXeDCaseUser'
                                   WHEN i % 1000 = 0 THEN 'rare' || lpad(i::text, 8, '0')
                                   ELSE 'user' || lpad(i::text, 8, '0') || substr(md5(i::text), 1, 8) END,
                              repeat('x', 60), TIMESTAMP '2026-01-01', '010-1234-5678',
                              'synthetic' || i || '@example.invalid'
                            FROM generate_series(1, __COUNT__) i
                            """.replace("__COUNT__", Integer.toString(count)));
                    execute(connection, "ANALYZE search_bench_users");
                    Map<String, List<Long>> expected = new LinkedHashMap<>();
                    for (var scenario : CASES.entrySet()) {
                        String pattern = pattern(scenario.getValue());
                        expected.put(scenario.getKey(), consume(connection, FULL, pattern));
                        if (!expected.get(scenario.getKey()).equals(consume(connection, PROJECTED, pattern)))
                            throw new AssertionError("Projection results differ: " + scenario.getKey());
                        if (!customOnly) comparePair(connection, count, scenario.getKey(), pattern, rows, output);
                    }
                    double baseWrite = insertBatch(connection);
                    long indexStart = System.nanoTime();
                    execute(connection, "CREATE INDEX search_bench_trgm ON search_bench_users USING gin (upper(login_id) gin_trgm_ops)");
                    double buildMs = millis(System.nanoTime() - indexStart);
                    execute(connection, "ANALYZE search_bench_users");
                    long indexBytes;
                    try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT pg_relation_size('search_bench_trgm')")) {
                        rs.next(); indexBytes = rs.getLong(1);
                    }
                    double indexedWrite = insertBatch(connection);
                    metadata.add("Phase=" + (customOnly ? "custom" : "default") + "; Users=" + count + "; GIN build ms=" + buildMs + "; bytes=" + indexBytes
                            + "; insert 1000 rows single observation: baseline ms=" + baseWrite + ", GIN ms=" + indexedWrite);
                    System.out.println("Dataset " + count + ": measuring projected + GIN");
                    if (customOnly) {
                        // Demonstrate whether the JDBC driver's cached statement uses a generic plan.
                        if (count == 100_000) {
                            measure(connection, PROJECTED, pattern("user"));
                            measure(connection, PROJECTED, pattern("rare00001000"));
                            cachedPlan(connection, pattern("rare00001000"), output.resolve("plans/100000-prepared-default.txt"));
                        }
                        execute(connection, "SET LOCAL plan_cache_mode = force_custom_plan");
                    }
                    for (var scenario : CASES.entrySet()) {
                        String pattern = pattern(scenario.getValue());
                        List<Long> found = consume(connection, PROJECTED, pattern);
                        if (!expected.get(scenario.getKey()).equals(found)) throw new AssertionError("Index results differ: " + scenario.getKey());
                        double[] samples = measure(connection, PROJECTED, pattern);
                        String variant = customOnly ? "projection_gin_custom" : "projection_gin";
                        rows.add(row(count, scenario.getKey(), variant, 3, found.size(), samples));
                        plan(connection, PROJECTED, pattern, output.resolve("plans/" + count + "-" + scenario.getKey() + "-" + variant + ".txt"));
                    }
                    Files.write(output.resolve("results.csv"), rows);
                    Files.write(output.resolve("environment.txt"), metadata);
                    System.out.println("Dataset " + count + ": completed; result equivalence passed");
                    execute(connection, "SET LOCAL plan_cache_mode = auto");
                }
            } finally {
                connection.rollback();
                System.out.println("Experiment transaction rolled back (fixtures, index, schema and any newly installed extension).");
            }
        }
    }

    private static void comparePair(Connection connection, int count, String name, String pattern,
                                    List<String> rows, Path output) throws Exception {
        double[] full = new double[RUNS], projected = new double[RUNS];
        for (int i = -WARMUP; i < RUNS; i++) {
            if (i % 2 == 0) {
                double a = timed(connection, FULL, pattern), b = timed(connection, PROJECTED, pattern);
                if (i >= 0) { full[i] = a; projected[i] = b; }
            } else {
                double b = timed(connection, PROJECTED, pattern), a = timed(connection, FULL, pattern);
                if (i >= 0) { full[i] = a; projected[i] = b; }
            }
        }
        int found = consume(connection, FULL, pattern).size();
        rows.add(row(count, name, "baseline_full", 7, found, full));
        rows.add(row(count, name, "projection", 3, found, projected));
        plan(connection, FULL, pattern, output.resolve("plans/" + count + "-" + name + "-baseline_full.txt"));
        plan(connection, PROJECTED, pattern, output.resolve("plans/" + count + "-" + name + "-projection.txt"));
    }

    private static double[] measure(Connection connection, String sql, String pattern) throws SQLException {
        double[] values = new double[RUNS];
        for (int i = -WARMUP; i < RUNS; i++) {
            double elapsed = timed(connection, sql, pattern);
            if (i >= 0) values[i] = elapsed;
        }
        return values;
    }

    private static double timed(Connection connection, String sql, String pattern) throws SQLException {
        long start = System.nanoTime(); consume(connection, sql, pattern); return millis(System.nanoTime() - start);
    }

    private static List<Long> consume(Connection connection, String sql, String pattern) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                    for (int i = 2; i <= rs.getMetaData().getColumnCount(); i++) rs.getObject(i);
                }
            }
        }
        return ids;
    }

    private static double insertBatch(Connection connection) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        long start = System.nanoTime();
        execute(connection, """
                INSERT INTO search_bench_users
                SELECT 2000000 + i, 'Write probe', 'writeprobe' || i, repeat('x', 60),
                  TIMESTAMP '2026-01-01', '010-1234-5678', 'write@example.invalid'
                FROM generate_series(1, 1000) i
                """);
        double elapsed = millis(System.nanoTime() - start);
        connection.rollback(savepoint); connection.releaseSavepoint(savepoint);
        return elapsed;
    }

    private static void plan(Connection connection, String sql, String pattern, Path output) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("EXPLAIN (ANALYZE, BUFFERS) " + sql)) {
            statement.setString(1, pattern);
            List<String> lines = new ArrayList<>(); lines.add("SQL: " + sql); lines.add("Pattern: " + pattern);
            try (ResultSet rs = statement.executeQuery()) { while (rs.next()) lines.add(rs.getString(1)); }
            Files.write(output, lines);
        }
    }

    private static void cachedPlan(Connection connection, String pattern, Path output) throws Exception {
        List<String> lines = new ArrayList<>();
        String name = null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name, generic_plans, custom_plans FROM pg_prepared_statements WHERE statement = ?")) {
            String serverSql = PROJECTED.replace("?", "$1");
            statement.setString(1, serverSql);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    name = rs.getString(1);
                    lines.add("Prepared=" + name + "; generic_plans=" + rs.getLong(2) + "; custom_plans=" + rs.getLong(3));
                }
            }
        }
        if (name != null) {
            String sql = "EXPLAIN (ANALYZE, BUFFERS) EXECUTE \"" + name.replace("\"", "\"\"")
                    + "\" ('" + pattern.replace("'", "''") + "')";
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) lines.add(rs.getString(1));
            }
        } else lines.add("No matching server-prepared statement was found; no generic-plan conclusion can be drawn.");
        Files.write(output, lines);
    }

    private static String pattern(String value) {
        return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
    private static String row(int count, String scenario, String variant, int columns, int found, double[] samples) {
        double[] sorted = samples.clone(); Arrays.sort(sorted);
        StringJoiner raw = new StringJoiner(";"); for (double value : samples) raw.add(String.format(Locale.ROOT, "%.4f", value));
        return String.format(Locale.ROOT, "%d,%s,%s,%d,%d,%.4f,%.4f,%s", count, scenario, variant, columns, found,
                sorted[(RUNS - 1) / 2], sorted[(int) Math.ceil(RUNS * .95) - 1], raw);
    }
    private static double millis(long nanos) { return nanos / 1_000_000.0; }
    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }
}
