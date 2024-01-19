package cpu

data class InMemoryRegisters(
  private var pc: Int16 = 0,
  private var af: Int16 = 0,
  private var bc: Int16 = 0,
  private var de: Int16 = 0,
  private var hl: Int16 = 0,
  private var sp: Int16 = 0,
  private var ime: Boolean = false,
  var callDepthForDebug: Int = 0,
) : Registers {
    override fun toString() = objToStringHex(this)

    override fun getAf(): Int16 = af
    override fun setAf(x: Int16) {
        af = x
    }

    override fun getBc(): Int16 = bc
    override fun setBc(x: Int16) {
        bc = x
    }

    override fun getDe(): Int16 = de
    override fun setDe(x: Int16) {
        de = x
    }

    override fun getHl(): Int16 = hl
    override fun setHl(x: Int16) {
        hl = x
    }

    override fun getSp(): Int16 = sp
    override fun setSp(x: Int16) {
        sp = x
    }

    override fun getPc(): Int16 = pc
    override fun setPc(x: Int16) {
        pc = x
    }

    override fun getIme() = ime
    override fun setIme(on: Boolean) {
        ime = on
    }

    private fun getFlagOn(bitPos: Int): Boolean = af.and(1.shl(bitPos)) > 0
    private fun setFlagOn(bitPos: Int, on: Boolean) {
        af = if (on) {
            af.or(1.shl(bitPos))
        } else {
            af.and(1.shl(bitPos).inv().and(0xFFFF))
        }
    }

    override fun isZeroOn(): Boolean = getFlagOn(7)
    override fun setZero(on: Boolean) = setFlagOn(7, on)
    override fun isSubtractionOn(): Boolean = getFlagOn(6)
    override fun setSubtraction(on: Boolean) = setFlagOn(6, on)
    override fun isHalfCarryOn(): Boolean = getFlagOn(5)
    override fun setHalfCarry(on: Boolean) = setFlagOn(5, on)
    override fun isCarryOn(): Boolean = getFlagOn(4)
    override fun setCarry(on: Boolean) = setFlagOn(4, on)

    override fun incCallDepthForDebug(diff: Int) {
        callDepthForDebug += diff
    }
}
