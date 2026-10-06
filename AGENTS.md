# Project collaboration rules

## Design before implementation

- For changes to screens, visual design, animations, or navigation flows, first share a concrete visual draft in the conversation.
- Wait for the user to choose or approve the draft before editing application source, routing, or styles. A request for a draft is not approval to implement it.
- Create drafts separately from application source. Read-only inspection and writing standalone drafts are permitted before approval.
- Once the user approves a specific draft, implement that scope without asking for the same approval again.
- Honor an explicit instruction to skip the draft for a particular task.

## SAI visual direction

- Use abstract circles, overlap, color, and motion to express connection.
- As the circles approach, form a thin liquid-like bridge before geometric contact, then widen it smoothly. Show this motion in the draft before implementation.
- Avoid personifying the circles with labels such as '나', '너', or a union formula unless the user explicitly requests them.
- The proposed entry flow is a public introduction page leading to login or signup, then the authenticated spaces view. Implement only after draft approval.

## Reminder hook

- `.codex/hooks.json` registers a reminder on SessionStart and UserPromptSubmit.
- The hook adds these workflow instructions to context; it does not mechanically verify approval or block every file write.
- Hook activation requires Codex's own review/trust flow. Do not bypass that flow or claim the hook is active merely because its files exist.
