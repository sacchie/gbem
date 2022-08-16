package ppu

// 160x144 pixel LCD
// 8x8 tiles (20x18)
// palette
// 3 layers: background, window, objects

// VRAM: $8000-97FF (0x1800(6144) bytes, 16 bytes/tile, 384 tiles)
// Tile maps: $9800-$9BFF, $9C00-$9FFF

// VRAM state, some I/O registers -> screen pixels
fun render() {
    // read VRAM

    // get Block0, OBJ0 tile data
    vram.

    // reconstruct pixel states
}

typealias Address = Int
typealias Int8 = Int
typealias Int2 = Int

interface VRAM {
    fun get(addr: Address): Int8

    fun getPixelAt(x: Int, y: Int, startAddress: Address): Int2
}

class VRAMImpl : VRAM {
    override fun get(addr: Address): Int8 {
        TODO("Not yet implemented")
    }
    override fun getPixelAt(x: Int, y: Int, startAddress: Address): Int2 {
        TODO("Not yet implemented")
    }
}