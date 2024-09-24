import {newEmulator, emulatorStep, getDrawnFrameCount, onJoypadInput} from 'gbem-wasm/packages/gbem-wasm-wasm-js/kotlin/gbem-wasm-wasm-js.mjs';

class Emulation {
    constructor(romUint8Array, cb) {
        this.id = newEmulator(romUint8Array, cb);
    }
    
    step() {
        emulatorStep(this.id);
    }
    
    drawnFrameCount() {
        return getDrawnFrameCount(this.id);
    }
    
    handleJoypadInput(button, pressed) {
        onJoypadInput(this.id, button, pressed)
    }
}

console.log('hello');
let emulation = null;

(async () => {
    const rom = await fetch('http://localhost:3000/rom.gb');
    const uaRom = new Uint8Array(await rom.arrayBuffer());
    emulation = new Emulation(uaRom, drawPoint);
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
    if (emulation === null) {
        return;
    }
    emulation.step();
}, 10);

let prevDrawnFrameCount = 0;
let prevTimeMs = Date.now()
setInterval(() => {
    if (emulation === null) {
        return;
    }
    const currDrawnFrameCount = emulation.drawnFrameCount();
    const currTimeMs = Date.now();
    const fps = (currDrawnFrameCount - prevDrawnFrameCount) / (currTimeMs - prevTimeMs) * 1000;
    prevDrawnFrameCount = currDrawnFrameCount;
    prevTimeMs = currTimeMs;

    document.getElementById('fps').innerText = fps.toString();
}, 1000)

document.addEventListener('keydown', e => {
    handleKey(emulation, e.code, true)
})
document.addEventListener('keyup', e => {
    handleKey(emulation, e.code, false)
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