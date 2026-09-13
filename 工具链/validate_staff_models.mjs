import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const assetRoot = path.join(root, 'src/main/resources/assets/elden_ring_spells');
const legalAngles = new Set([-45, -22.5, 0, 22.5, 45]);
const handTransforms = {
    thirdperson_righthand: {translation: [0, 3, 1.25]},
    thirdperson_lefthand: {translation: [0, 3, 1.25]},
    firstperson_righthand: {translation: [1.5, 0, -2.75]},
    firstperson_lefthand: {translation: [1.5, 0, -2.75]},
};
const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const results = [];

for (const name of ['astrologer_staff', 'azur_glintstone_staff']) {
    const model = JSON.parse(readFileSync(path.join(assetRoot, 'models/item', name + '.json'), 'utf8'));
    const editable = JSON.parse(readFileSync(path.join(root, '模型', name + '.bbmodel'), 'utf8'));
    const texture = readFileSync(path.join(assetRoot, 'textures/item', name + '.png'));
    assert.equal(texture.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
    assert.deepEqual([texture.readUInt32BE(16), texture.readUInt32BE(20)], [32, 32]);
    assert.deepEqual(model.texture_size, [32, 32]);
    assert.equal(model.textures['0'], 'elden_ring_spells:item/' + name);
    assert.equal(model.textures.particle, model.textures['0']);
    assert.equal(editable.meta.model_format, 'java_block');
    assert.equal(editable.elements.length, model.elements.length);
    assert.equal(editable.textures.length, 1);
    assert.deepEqual([editable.textures[0].width, editable.textures[0].height], [32, 32]);
    assert(!editable.textures[0].path && !editable.textures[0].relative_path);
    assert.equal(digest(Buffer.from(editable.textures[0].source.split(',')[1], 'base64')), digest(texture));
    for (const [slot, transform] of Object.entries(handTransforms)) assert.deepEqual(model.display[slot], transform);

    const bounds = {min: [Infinity, Infinity, Infinity], max: [-Infinity, -Infinity, -Infinity]};
    let faceCount = 0;
    for (const element of model.elements) {
        for (let axis = 0; axis < 3; axis++) {
            assert(Number.isFinite(element.from[axis]) && Number.isFinite(element.to[axis]));
            assert(element.from[axis] >= -16 && element.to[axis] <= 32);
            assert(element.from[axis] < element.to[axis]);
        }
        if (element.rotation) {
            assert(legalAngles.has(element.rotation.angle));
            assert(['x', 'y', 'z'].includes(element.rotation.axis));
            assert.equal(element.rotation.origin.length, 3);
            assert(element.rotation.origin.every(Number.isFinite));
        }
        assert.equal(Object.keys(element.faces).length, 6);
        for (const face of Object.values(element.faces)) {
            assert.equal(face.texture, '#0');
            assert.equal(face.uv.length, 4);
            assert(face.uv.every(value => Number.isFinite(value) && value >= 0 && value <= 16));
            assert.notEqual(face.uv[0], face.uv[2]);
            assert.notEqual(face.uv[1], face.uv[3]);
            faceCount++;
        }
        // Test all rotated corners, not just the unrotated element endpoints.
        for (let corner = 0; corner < 8; corner++) {
            const point = element.from.map((value, axis) => corner & (1 << axis) ? element.to[axis] : value);
            if (element.rotation?.angle) {
                const rotation = element.rotation;
                const axis = ['x', 'y', 'z'].indexOf(rotation.axis);
                const first = (axis + 1) % 3;
                const second = (axis + 2) % 3;
                const radians = rotation.angle * Math.PI / 180;
                const firstValue = point[first] - rotation.origin[first];
                const secondValue = point[second] - rotation.origin[second];
                point[first] = rotation.origin[first] + firstValue * Math.cos(radians) - secondValue * Math.sin(radians);
                point[second] = rotation.origin[second] + firstValue * Math.sin(radians) + secondValue * Math.cos(radians);
            }
            point.forEach((value, axis) => {
                assert(value >= -16 && value <= 32);
                bounds.min[axis] = Math.min(bounds.min[axis], value);
                bounds.max[axis] = Math.max(bounds.max[axis], value);
            });
        }
    }
    results.push({name, cubes: model.elements.length, faces: faceCount, texture: '32x32',
        textureBytes: texture.length, textureSha256: digest(texture),
        bounds: Object.fromEntries(Object.entries(bounds).map(([key, values]) => [key, values.map(value => +value.toFixed(4))])),
        handTransforms: 'unchanged', embeddedTexture: 'identical', result: 'PASS'});
}
process.stdout.write(JSON.stringify(results, null, 2) + '\n');
