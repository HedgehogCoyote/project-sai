// Souvenir shapes for the room's wall shelf. Built from the engine's own rounded boxes and
// spheres so they share its matte look; photo holders show the Foot Print photo.
import * as THREE from './vendor/three.module.js';

const textures = new Map();
const loader = new THREE.TextureLoader();
loader.setCrossOrigin('use-credentials');

/** Square-cropped photo texture, cached by URL; `onLoad` repaints when the image arrives. */
function photoTexture(url, onLoad) {
  if (textures.has(url)) return textures.get(url);
  const tex = loader.load(url, t => {
    const img = t.image, w = img.width || 1, h = img.height || 1;
    if (w > h) { t.repeat.set(h / w, 1); t.offset.set((1 - h / w) / 2, 0); }
    else { t.repeat.set(1, w / h); t.offset.set(0, (1 - w / h) / 2); }
    t.needsUpdate = true;
    onLoad();
  }, undefined, () => onLoad());
  tex.colorSpace = THREE.SRGBColorSpace;
  textures.set(url, tex);
  return tex;
}

function photoPlane(parent, url, size, x, y, z, onLoad) {
  const mat = url
    ? new THREE.MeshStandardMaterial({ map: photoTexture(url, onLoad), roughness: .8 })
    : new THREE.MeshStandardMaterial({ color: '#c9d6de', roughness: .9 });
  const mesh = new THREE.Mesh(new THREE.PlaneGeometry(size, size), mat);
  mesh.position.set(x, y, z);
  parent.add(mesh);
  return mesh;
}

function cylinder(h, parent, r, height, color, x, y, z) {
  const mesh = new THREE.Mesh(new THREE.CylinderGeometry(r, r, height, 20), h.material(color));
  mesh.position.set(x, y, z);
  mesh.castShadow = true;
  parent.add(mesh);
  return mesh;
}

/** Returns the item's height so the engine can place its label above it. */
export function buildSouvenir(h, parent, kind, colors, photoUrl, onLoad) {
  const { rounded, sphere } = h;
  const a = colors?.primary ?? '#aa849c', b = colors?.secondary ?? '#71576c';
  switch (kind) {
    case 'polaroid': {
      const p = new THREE.Group(); p.rotation.x = -.12; parent.add(p);
      rounded(p, .42, .5, .025, '#fbf8f2', 0, .27, 0, .01);
      photoPlane(p, photoUrl, .34, 0, .3, .016, onLoad);
      return .55;
    }
    case 'frame': {
      rounded(parent, .52, .58, .06, '#bf936b', 0, .3, 0, .02);
      rounded(parent, .44, .5, .02, '#f6eedf', 0, .3, .035, .008);
      photoPlane(parent, photoUrl, .36, 0, .31, .047, onLoad);
      return .6;
    }
    case 'acrylic': {
      const clear = new THREE.MeshPhysicalMaterial({ color: '#ffffff', roughness: .08, transparent: true, opacity: .3, clearcoat: 1 });
      const base = new THREE.Mesh(new THREE.BoxGeometry(.42, .04, .2), clear); base.position.y = .02; parent.add(base);
      const plate = new THREE.Mesh(new THREE.BoxGeometry(.44, .5, .02), clear); plate.position.y = .3; parent.add(plate);
      photoPlane(parent, photoUrl, .4, 0, .3, .012, onLoad);
      return .56;
    }
    case 'shell':
      sphere(parent, '#f2c6c0', -.06, .07, 0, .13, .07, .11); sphere(parent, '#f7e3d2', .12, .05, .05, .08, .05, .07);
      return .16;
    case 'ticket':
      rounded(parent, .38, .02, .2, '#f4e7c6', 0, .02, 0, .005); rounded(parent, .38, .021, .04, b, 0, .025, -.05, .004);
      return .1;
    case 'lantern': {
      rounded(parent, .22, .34, .22, a, 0, .17, 0, .04);
      const glow = new THREE.Mesh(new THREE.SphereGeometry(.07, 16, 12), new THREE.MeshStandardMaterial({ color: '#ffe3ae', emissive: '#ffc57e', emissiveIntensity: 1.6 }));
      glow.position.y = .2; parent.add(glow);
      rounded(parent, .26, .04, .26, b, 0, .36, 0, .015);
      return .42;
    }
    case 'cake':
      cylinder(h, parent, .2, .14, '#fff4e8', 0, .07, 0); cylinder(h, parent, .15, .12, '#fbe0e8', 0, .2, 0);
      for (const x of [-.06, 0, .06]) rounded(parent, .02, .08, .02, b, x, .3, 0, .005);
      return .36;
    case 'trophy':
      rounded(parent, .26, .06, .2, '#8f6b4f', 0, .03, 0, .02); rounded(parent, .05, .14, .05, '#d9b04c', 0, .13, 0, .01);
      sphere(parent, '#e6c160', 0, .3, 0, .13, .12, .13);
      return .44;
    case 'medal':
      rounded(parent, .08, .3, .02, b, 0, .3, 0, .005); cylinder(h, parent, .1, .025, '#e2b64a', 0, .13, .01).rotation.x = Math.PI / 2;
      return .46;
    case 'ball':
      sphere(parent, '#fbfbf6', 0, .16, 0, .16);
      for (const [x, y, z] of [[0, .3, 0], [.13, .19, .07], [-.12, .2, .08], [.02, .17, .15]]) sphere(parent, '#4b4a52', x, y, z, .045, .045, .02);
      return .32;
    case 'jar': {
      const glass = new THREE.Mesh(new THREE.CylinderGeometry(.13, .13, .32, 20), new THREE.MeshPhysicalMaterial({ color: '#ffffff', transparent: true, opacity: .25, roughness: .1 }));
      glass.position.y = .16; parent.add(glass);
      rounded(parent, .27, .05, .27, '#bf936b', 0, .34, 0, .02);
      for (const [x, y, z, c] of [[-.04, .05, 0, a], [.05, .06, .03, b], [0, .12, -.04, '#f3d36b'], [.03, .17, .02, a]]) sphere(parent, c, x, y, z, .045);
      return .4;
    }
    case 'plant':
      rounded(parent, .24, .2, .24, b, 0, .1, 0, .04); sphere(parent, '#86a873', 0, .3, 0, .14); sphere(parent, '#9cbc86', .07, .4, .03, .09);
      return .48;
    default:
      rounded(parent, .3, .3, .3, a, 0, .15, 0, .05);
      return .32;
  }
}

/** Mixes two #rrggbb colours; t = 0 keeps `a`. */
export function mix(a, b, t) {
  const p = c => [1, 3, 5].map(i => parseInt(c.slice(i, i + 2), 16));
  const x = p(a), y = p(b);
  return '#' + x.map((v, i) => Math.round(v + (y[i] - v) * t).toString(16).padStart(2, '0')).join('');
}
