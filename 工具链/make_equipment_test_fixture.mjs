import { gzipSync } from 'node:zlib';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

// One-block empty NBT structure, used only by the isolated equipment GameTests.
const utf = text => {
  const value = Buffer.from(text, 'utf8');
  const length = Buffer.alloc(2);
  length.writeUInt16BE(value.length);
  return Buffer.concat([length, value]);
};
const int = value => {
  const bytes = Buffer.alloc(4);
  bytes.writeInt32BE(value);
  return bytes;
};
const tag = (type, name, value) => Buffer.concat([Buffer.from([type]), utf(name), value]);
const structure = Buffer.concat([
  Buffer.from([10, 0, 0]),
  tag(3, 'DataVersion', int(3955)),
  tag(9, 'size', Buffer.concat([Buffer.from([3]), int(3), int(1), int(1), int(1)])),
  tag(9, 'palette', Buffer.concat([Buffer.from([10]), int(1), tag(8, 'Name', utf('minecraft:air')), Buffer.from([0])])),
  tag(9, 'blocks', Buffer.concat([Buffer.from([10]), int(0)])),
  tag(9, 'entities', Buffer.concat([Buffer.from([10]), int(0)])),
  Buffer.from([0])
]);
const path = resolve('src/gametest/resources/data/elden_ring_spells/structure/empty.nbt');
mkdirSync(dirname(path), { recursive: true });
writeFileSync(path, gzipSync(structure));
console.log(path);
