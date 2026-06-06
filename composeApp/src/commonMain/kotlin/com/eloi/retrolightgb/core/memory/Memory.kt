package com.eloi.retrolightgb.core.memory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eloi.retrolightgb.SaveManager
import com.eloi.retrolightgb.core.apu.Apu
import com.eloi.retrolightgb.core.cpu.InterruptType

@OptIn(ExperimentalUnsignedTypes::class)
class Memory(private val apu: Apu? = null) {
    private val ram = UByteArray(RAM_SIZE)
    private val bootRom: UByteArray = BootRom.data.map { byte -> byte.toUByte() }.toUByteArray()
    private var isBootRomEnabled = true
    private var ifRegister: UByte = 0u
    private var ieRegister: UByte = 0u
    private var cartridge: Cartridge? = null
    private var saveName: String? = null

    private val serialBuffer = StringBuilder()
    var serialOutput by mutableStateOf("")
        private set

    private var divCounter: Int = 0
    private var timerAccumulator: Int = 0

    private var joypadSelection: UByte = 0x30u
    private val pressedButtons = mutableSetOf<JoypadButton>()

    fun pressButton(button: JoypadButton) {
        if (pressedButtons.add(button)) requestInterrupt(InterruptType.Joypad)
    }

    fun releaseButton(button: JoypadButton) {
        pressedButtons.remove(button)
    }

    fun load(rom: ByteArray) {
        save()  // persist current game before replacing it
        reset()
        val uRom = UByteArray(rom.size) { rom[it].toUByte() }
        saveName = extractRomTitle(uRom)
        cartridge = CartridgeFactory.create(uRom)
        if (CartridgeFactory.hasBattery(uRom)) {
            saveName?.let { name -> SaveManager.load(name)?.let { cartridge?.loadRam(it) } }
        }
    }

    fun save() {
        val name = saveName ?: return
        val data = cartridge?.saveRam() ?: return
        SaveManager.save(name, data)
    }

    private fun extractRomTitle(rom: UByteArray): String =
        (0x0134..0x0143)
            .map { if (it < rom.size) rom[it].toInt() else 0 }
            .takeWhile { it != 0 }
            .joinToString("") { it.toChar().toString() }
            .trim()
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .ifEmpty { "unknown" }

    fun reset() {
        ram.fill(0u)
        isBootRomEnabled = true
        ifRegister = 0u
        ieRegister = 0u
        divCounter = 0
        timerAccumulator = 0
        joypadSelection = 0x30u
        pressedButtons.clear()
        serialBuffer.clear()
        serialOutput = ""
        cartridge = null
        saveName = null
    }

    fun requestInterrupt(type: InterruptType) {
        ifRegister = ifRegister or type.mask
    }

    fun clearInterruptRequest(type: InterruptType) {
        ifRegister = ifRegister and type.mask.inv()
    }

    fun readByte(address: UShort): UByte {
        val addr = address.toInt()
        return when {
            isBootRomEnabled && addr in 0 until bootRom.size -> bootRom[addr]
            addr in 0x0000..0x7FFF -> cartridge?.readByte(addr) ?: 0xFFu
            addr in 0xA000..0xBFFF -> cartridge?.readByte(addr) ?: 0xFFu
            addr == 0xFF00 -> {
                var nibble = 0x0F
                if (joypadSelection and 0x20u.toUByte() == 0u.toUByte()) {
                    if (JoypadButton.A      in pressedButtons) nibble = nibble and 0x0E
                    if (JoypadButton.B      in pressedButtons) nibble = nibble and 0x0D
                    if (JoypadButton.Select in pressedButtons) nibble = nibble and 0x0B
                    if (JoypadButton.Start  in pressedButtons) nibble = nibble and 0x07
                }
                if (joypadSelection and 0x10u.toUByte() == 0u.toUByte()) {
                    if (JoypadButton.Right in pressedButtons) nibble = nibble and 0x0E
                    if (JoypadButton.Left  in pressedButtons) nibble = nibble and 0x0D
                    if (JoypadButton.Up    in pressedButtons) nibble = nibble and 0x0B
                    if (JoypadButton.Down  in pressedButtons) nibble = nibble and 0x07
                }
                (0xC0 or joypadSelection.toInt() or nibble).toUByte()
            }
            addr in 0xFF10..0xFF3F -> apu?.readByte(addr) ?: ram[addr]
            addr == 0xFF0F -> (ifRegister or 0xE0u)
            addr == 0xFFFF -> ieRegister
            else -> ram[addr]
        }
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt()

        if (addr == BOOT_DISABLED_ADDRESS && value == 0x01u.toUByte())
            isBootRomEnabled = false

        when (addr) {
            in 0x0000..0x7FFF -> cartridge?.writeByte(addr, value)
            in 0xA000..0xBFFF -> cartridge?.writeByte(addr, value)
            0xFF00 -> joypadSelection = value and 0x30u.toUByte()
            0xFF04 -> { divCounter = 0; timerAccumulator = 0; ram[addr] = 0u }
            0xFF46 -> {
                val srcBase = value.toInt() shl 8
                for (i in 0 until 160) ram[0xFE00 + i] = readByte((srcBase + i).toUShort())
            }
            in 0xFF10..0xFF3F -> apu?.writeByte(addr, value) ?: run { ram[addr] = value }
            0xFF0F -> ifRegister = value
            0xFFFF -> ieRegister = value
            // Serial transfer: bit 7 = start, bit 0 = internal clock
            0xFF02 -> {
                ram[addr] = value
                if (value and 0x81u.toUByte() == 0x81u.toUByte()) {
                    serialBuffer.append(ram[0xFF01].toInt().toChar())
                    serialOutput = serialBuffer.toString()
                    ram[addr] = value and 0x7Fu.toUByte() // clear transfer-start bit
                    requestInterrupt(InterruptType.Serial)
                }
            }
            else -> ram[addr] = value
        }
    }

    fun tickTimer(cycles: Int) {
        cartridge?.tick(cycles)
        divCounter += cycles
        ram[0xFF04] = ((divCounter shr 8) and 0xFF).toUByte()

        val tac = ram[0xFF07].toInt()
        if ((tac and 0x04) == 0) return

        val clockFreq = when (tac and 0x03) {
            0 -> 1024
            1 -> 16
            2 -> 64
            else -> 256
        }

        timerAccumulator += cycles
        while (timerAccumulator >= clockFreq) {
            timerAccumulator -= clockFreq
            val tima = ram[0xFF05].toInt()
            if (tima == 0xFF) {
                ram[0xFF05] = ram[0xFF06]
                requestInterrupt(InterruptType.Timer)
            } else {
                ram[0xFF05] = (tima + 1).toUByte()
            }
        }
    }

    companion object {
        private const val RAM_SIZE = 0x10000
        private const val BOOT_DISABLED_ADDRESS = 0xFF50
    }
}
