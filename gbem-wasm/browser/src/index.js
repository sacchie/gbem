import {makeEmulator, emulatorStep, emulatorDrawnFrameCount, emulatorOnJoypadInput} from 'gbem-wasm/packages/gbem-wasm-wasm-js/kotlin/gbem-wasm-wasm-js.mjs';

class Emulator {
    constructor(romUint8Array, cb) {
        this.id = makeEmulator(romUint8Array, cb);
    }
    
    step() {
        emulatorStep(this.id);
    }
    
    drawnFrameCount() {
        return emulatorDrawnFrameCount(this.id);
    }
    
    handleJoypadInput(button, pressed) {
        emulatorOnJoypadInput(this.id, button, pressed)
    }
}

console.log('hello');
let emulator = null;

(async () => {
    const rom = await fetch('http://localhost:3000/rom.gb');
    const uaRom = new Uint8Array(await rom.arrayBuffer());
    emulator = new Emulator(uaRom, drawPoint);
})();

const zoom = 3
const W = 160
const H = 144
const canvas = document.getElementById("emulation");
canvas.style.width = `${zoom * W}px`
canvas.style.height = `${zoom * H}px`
const ctx = canvas.getContext("2d");

function drawPoint(x, y, color) {
    ctx.fillStyle = color === 0 ? 'white' : color === 1 ? 'lightgreen' : color === 2 ? 'green' : 'black';
    ctx.fillRect(x * zoom, y * zoom, zoom, zoom);
}

setInterval(() => {
    if (emulator === null) {
        return;
    }
    emulator.step();
}, 10);

let prevDrawnFrameCount = 0;
let prevTimeMs = Date.now()
setInterval(() => {
    if (emulator === null) {
        return;
    }
    const currDrawnFrameCount = emulator.drawnFrameCount();
    const currTimeMs = Date.now();
    const fps = (currDrawnFrameCount - prevDrawnFrameCount) / (currTimeMs - prevTimeMs) * 1000;
    prevDrawnFrameCount = currDrawnFrameCount;
    prevTimeMs = currTimeMs;

    document.getElementById('fps').innerText = fps.toString();
}, 1000)

document.addEventListener('keydown', e => {
    handleKey(emulator, e.code, true)
})
document.addEventListener('keyup', e => {
    handleKey(emulator, e.code, false)
});

function handleKey(emulation, code, pressed) {
    switch (code) {
        case "ArrowUp":
            emulation.handleJoypadInput('Up', pressed);
            break;
        case "ArrowDown":
            emulation.handleJoypadInput('Down', pressed);
            break;
        case "ArrowLeft":
            emulation.handleJoypadInput('Left', pressed);
            break;
        case "ArrowRight":
            emulation.handleJoypadInput('Right', pressed);
            break;
        case "KeyX":
            emulation.handleJoypadInput('A', pressed);
            break;
        case "KeyZ":
            emulation.handleJoypadInput('B', pressed);
            break;
        case "KeyA":
            emulation.handleJoypadInput('Start', pressed);
            break;
        case "KeyS":
            emulation.handleJoypadInput('Select', pressed);
            break;
    }
}