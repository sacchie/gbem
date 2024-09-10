import {emulate} from 'gbem-wasm/packages/gbem-wasm-wasm-js/kotlin/gbem-wasm-wasm-js.mjs';

console.log('hello');

(async () => {
    const rom = await fetch('http://localhost:3000/rom.gb');
    const ua = new Uint8Array(await rom.arrayBuffer());
    setTimeout(() => emulate(ua, cb), 0);
    console.log('ran')
})();

const canvas = document.getElementById("emulation");
const buffer = new Array(160 * 144);

let flag = false;

function cb(x, y, color) {
    // console.log({x, y, color});
    buffer[y * 160 + x] = color;
    flag = true;
}

setInterval(() => {
    if (!flag) {
        return;
    }
    console.log('hello')
    for (let y = 0; y < 144; y++) {
        for (let x = 0; x < 160; x++) {
            const ctx = canvas.getContext("2d");
            const color = buffer[y * 160 + x]
            ctx.fillStyle = color === 0 ? 'white' : color === 1 ? 'lightgreen' : color === 2 ? 'green' : 'black';
            ctx.fillRect(x, y, 1, 1);
        }
    }
}, 100);
