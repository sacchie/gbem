import {newEmulator, emulatorStep, getDrawnFrameCount} from 'gbem-wasm/packages/gbem-wasm-wasm-js/kotlin/gbem-wasm-wasm-js.mjs';

console.log('hello');
let id = null;

(async () => {
    const rom = await fetch('http://localhost:3000/rom.gb');
    const ua = new Uint8Array(await rom.arrayBuffer());
    id = newEmulator(ua, cb);
})();

const canvas = document.getElementById("emulation");
const buffer = new Array(160 * 144);

function cb(x, y, color) {
    // console.log({x, y, color});
    buffer[y * 160 + x] = color;
}

setInterval(() => {
   for (let y = 0; y < 144; y++) {
        for (let x = 0; x < 160; x++) {
            const ctx = canvas.getContext("2d");
            const color = buffer[y * 160 + x]
            ctx.fillStyle = color === 0 ? 'white' : color === 1 ? 'lightgreen' : color === 2 ? 'green' : 'black';
            ctx.fillRect(x, y, 1, 1);
        }
    }
}, 1000);

setInterval(() => {
    if (id === null) {
        return;
    }
    emulatorStep(id);
}, 1);

let prevDrawnFrameCount = 0;
let prevTimeMs = Date.now()
setInterval(() => {
    if (id === null) {
        return;
    }
    const currDrawnFrameCount = getDrawnFrameCount(id);
    const currTimeMs = Date.now();
    const fps = (currDrawnFrameCount - prevDrawnFrameCount) / (currTimeMs - prevTimeMs) * 1000;
    prevDrawnFrameCount = currDrawnFrameCount;
    prevTimeMs = currTimeMs;

    document.getElementById('fps').innerText = fps.toString();
}, 1000)
