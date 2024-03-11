package com.onandor.peripheryapp.kbm.input

data class Key(
    val scanCode: Int,
    val modifier: Int = KeyMapping.Modifiers.NONE
)

class KeyMapping {

    object Modifiers {
        val NONE = 0x00
        val L_CTRL = 0x01
        val L_SHIFT = 0x02
        val L_ALT = 0x04
        val L_META = 0x08
        val R_CTRL = 0x10
        val R_SHIFT = 0x20
        val R_ALT = 0x40
        val R_META = 0x80
    }

    object SpecialKeys {
        val RETURN = 0x28
        val ESCAPE = 0x29
        val BACKSPACE = 0x2A
        val TAB = 0x2B
        val DELETE = 0x4C
    }

    object ArrowKeys {
        val RIGHT = 0x4F
        val LEFT = 0x50
        val DOWN = 0x51
        val UP = 0x52
    }

    companion object {

        val BACKSPACE = "BACKSPACE"

        // Android key code
        val keyCodeMap_EnUs = mapOf(
            0x07 to Key(0x27),   // 0
            0x08 to Key(0x1E),   // 1
            0x09 to Key(0x1F),   // 2
            0x0A to Key(0x20),   // 3
            0x0B to Key(0x21),   // 4
            0x0C to Key(0x22),   // 5
            0x0D to Key(0x23),   // 6
            0x0E to Key(0x24),   // 7
            0x0F to Key(0x25),   // 8
            0x10 to Key(0x26),   // 9
            0x11 to Key(0x25, Modifiers.L_SHIFT),   // *
            0x12 to Key(0x20, Modifiers.L_SHIFT),   // #
            0x1D to Key(0x04),   // A
            0x1E to Key(0x05),   // B
            0x1F to Key(0x06),   // C
            0x20 to Key(0x07),   // D
            0x21 to Key(0x08),   // E
            0x22 to Key(0x09),   // F
            0x23 to Key(0x0A),   // G
            0x24 to Key(0x0B),   // H
            0x25 to Key(0x0C),   // I
            0x26 to Key(0x0D),   // J
            0x27 to Key(0x0E),   // K
            0x28 to Key(0x0F),   // L
            0x29 to Key(0x10),   // M
            0x2A to Key(0x11),   // N
            0x2B to Key(0x12),   // O
            0x2C to Key(0x13),   // P
            0x2D to Key(0x14),   // Q
            0x2E to Key(0x15),   // R
            0x2F to Key(0x16),   // S
            0x30 to Key(0x17),   // T
            0x31 to Key(0x18),   // U
            0x32 to Key(0x19),   // V
            0x33 to Key(0x1A),   // W
            0x34 to Key(0x1B),   // X
            0x35 to Key(0x1C),   // Y
            0x36 to Key(0x1D),   // Z
            0x37 to Key(0x36),   // ,
            0x38 to Key(0x37),   // .
            0x3D to Key(0x2B),   // Tab
            0x3E to Key(0x2C),   // Space
            0x42 to Key(0x28),   // Return
            0x43 to Key(0x2A),   // Backspace
            0x44 to Key(0x35),   // `
            0x45 to Key(0x2D),   // -
            0x46 to Key(0x2E),   // =
            0x47 to Key(0x2F),   // [
            0x48 to Key(0x30),   // ]
            0x49 to Key(0x31),   // \
            0x4A to Key(0x33),   // ;
            0x4B to Key(0x34),   // '
            0x4C to Key(0x38),   // /
            0x4D to Key(0x1F, Modifiers.L_SHIFT),   // @
            0x51 to Key(0x2E, Modifiers.L_SHIFT),    // +
            0x6F to Key(0x29),   // Escape
            0x70 to Key(0x4C)    // Delete
        )

        val shiftKeyCodeMap_EnUs = mapOf(
            0x07 to Key(0x27, Modifiers.L_SHIFT),   // )
            0x08 to Key(0x1E, Modifiers.L_SHIFT),   // !
            0x0B to Key(0x21, Modifiers.L_SHIFT),   // $
            0x0C to Key(0x22, Modifiers.L_SHIFT),   // %
            0x0D to Key(0x23, Modifiers.L_SHIFT),   // ^
            0x0E to Key(0x24, Modifiers.L_SHIFT),   // &
            0x10 to Key(0x26, Modifiers.L_SHIFT),   // (
            0x1D to Key(0x04, Modifiers.L_SHIFT),   // A
            0x1E to Key(0x05, Modifiers.L_SHIFT),   // B
            0x1F to Key(0x06, Modifiers.L_SHIFT),   // C
            0x20 to Key(0x07, Modifiers.L_SHIFT),   // D
            0x21 to Key(0x08, Modifiers.L_SHIFT),   // E
            0x22 to Key(0x09, Modifiers.L_SHIFT),   // F
            0x23 to Key(0x0A, Modifiers.L_SHIFT),   // G
            0x24 to Key(0x0B, Modifiers.L_SHIFT),   // H
            0x25 to Key(0x0C, Modifiers.L_SHIFT),   // I
            0x26 to Key(0x0D, Modifiers.L_SHIFT),   // J
            0x27 to Key(0x0E, Modifiers.L_SHIFT),   // K
            0x28 to Key(0x0F, Modifiers.L_SHIFT),   // L
            0x29 to Key(0x10, Modifiers.L_SHIFT),   // M
            0x2A to Key(0x11, Modifiers.L_SHIFT),   // N
            0x2B to Key(0x12, Modifiers.L_SHIFT),   // O
            0x2C to Key(0x13, Modifiers.L_SHIFT),   // P
            0x2D to Key(0x14, Modifiers.L_SHIFT),   // Q
            0x2E to Key(0x15, Modifiers.L_SHIFT),   // R
            0x2F to Key(0x16, Modifiers.L_SHIFT),   // S
            0x30 to Key(0x17, Modifiers.L_SHIFT),   // T
            0x31 to Key(0x18, Modifiers.L_SHIFT),   // U
            0x32 to Key(0x19, Modifiers.L_SHIFT),   // V
            0x33 to Key(0x1A, Modifiers.L_SHIFT),   // W
            0x34 to Key(0x1B, Modifiers.L_SHIFT),   // X
            0x35 to Key(0x1C, Modifiers.L_SHIFT),   // Y
            0x36 to Key(0x1D, Modifiers.L_SHIFT),   // Z
            0x37 to Key(0x36, Modifiers.L_SHIFT),   // <
            0x38 to Key(0x37, Modifiers.L_SHIFT),   // >
            0x3D to Key(0x2B),   // Tab
            0x3E to Key(0x2C),   // Space
            0x42 to Key(0x28),   // Return
            0x43 to Key(0x2A),   // Backspace
            0x44 to Key(0x35, Modifiers.L_SHIFT),   // ~
            0x45 to Key(0x2D, Modifiers.L_SHIFT),   // _
            0x47 to Key(0x2F, Modifiers.L_SHIFT),   // {
            0x48 to Key(0x30, Modifiers.L_SHIFT),   // }
            0x49 to Key(0x31, Modifiers.L_SHIFT),   // |
            0x4A to Key(0x33, Modifiers.L_SHIFT),   // :
            0x4B to Key(0x34, Modifiers.L_SHIFT),   // "
            0x4C to Key(0x38, Modifiers.L_SHIFT),   // ?
            0x6F to Key(0x29),   // Escape
            0x70 to Key(0x4C)    // Delete
        )

        val shiftKeyCodeToScanCode_EnUs = mapOf(
            0x4D to 0x1F,   // @
            0x12 to 0x20,   // #
            0x11 to 0x25,   // *
            0x51 to 0x2E    // +
        )

        val keyCodeMap_HuHu = mapOf(
            0x07 to Key(0x35),   // 0
            0x08 to Key(0x1E),   // 1
            0x09 to Key(0x1F),   // 2
            0x0A to Key(0x20),   // 3
            0x0B to Key(0x21),   // 4
            0x0C to Key(0x22),   // 5
            0x0D to Key(0x23),   // 6
            0x0E to Key(0x24),   // 7
            0x0F to Key(0x25),   // 8
            0x10 to Key(0x26),   // 9
            0x11 to Key(0x38, Modifiers.R_ALT),   // *
            0x12 to Key(0x1B, Modifiers.R_ALT),   // #
            0x1D to Key(0x04),   // A
            0x1E to Key(0x05),   // B
            0x1F to Key(0x06),   // C
            0x20 to Key(0x07),   // D
            0x21 to Key(0x08),   // E
            0x22 to Key(0x09),   // F
            0x23 to Key(0x0A),   // G
            0x24 to Key(0x0B),   // H
            0x25 to Key(0x0C),   // I
            0x26 to Key(0x0D),   // J
            0x27 to Key(0x0E),   // K
            0x28 to Key(0x0F),   // L
            0x29 to Key(0x10),   // M
            0x2A to Key(0x11),   // N
            0x2B to Key(0x12),   // O
            0x2C to Key(0x13),   // P
            0x2D to Key(0x14),   // Q
            0x2E to Key(0x15),   // R
            0x2F to Key(0x16),   // S
            0x30 to Key(0x17),   // T
            0x31 to Key(0x18),   // U
            0x32 to Key(0x19),   // V
            0x33 to Key(0x1A),   // W
            0x34 to Key(0x1B),   // X
            0x35 to Key(0x1D),   // Y
            0x36 to Key(0x1C),   // Z
            0x37 to Key(0x36),   // ,
            0x38 to Key(0x37),   // .
            0x3D to Key(0x2B),   // Tab
            0x3E to Key(0x2C),   // Space
            0x42 to Key(0x28),   // Return
            0x43 to Key(0x2A),   // Backspace
            0x44 to Key(0x24, Modifiers.R_ALT),   // `
            0x45 to Key(0x38),   // -
            0x46 to Key(0x24, Modifiers.L_SHIFT),   // =
            0x47 to Key(0x09, Modifiers.R_ALT),   // [
            0x48 to Key(0x0A, Modifiers.R_ALT),   // ]
            0x49 to Key(0x14, Modifiers.R_ALT),   // \
            0x4A to Key(0x36, Modifiers.R_ALT),   // ;
            0x4B to Key(0x1E, Modifiers.L_SHIFT),   // '
            0x4C to Key(0x23, Modifiers.L_SHIFT),   // /
            0x4D to Key(0x19, Modifiers.R_ALT),   // @
            0x51 to Key(0x20, Modifiers.L_SHIFT),    // +
            0x6F to Key(0x29),   // Escape
            0x70 to Key(0x4C)    // Delete
        )

        val shiftKeyCodeMap_HuHu = mapOf(
            0x07 to Key(0x26, Modifiers.L_SHIFT),   // )
            0x08 to Key(0x21, Modifiers.L_SHIFT),   // !
            0x0B to Key(0x33, Modifiers.R_ALT),   // $
            0x0C to Key(0x22, Modifiers.L_SHIFT),   // %
            0x0D to Key(0x20, Modifiers.R_ALT),   // ^
            0x0E to Key(0x06, Modifiers.R_ALT),   // &
            0x10 to Key(0x25, Modifiers.L_SHIFT),   // (
            0x1D to Key(0x04, Modifiers.L_SHIFT),   // A
            0x1E to Key(0x05, Modifiers.L_SHIFT),   // B
            0x1F to Key(0x06, Modifiers.L_SHIFT),   // C
            0x20 to Key(0x07, Modifiers.L_SHIFT),   // D
            0x21 to Key(0x08, Modifiers.L_SHIFT),   // E
            0x22 to Key(0x09, Modifiers.L_SHIFT),   // F
            0x23 to Key(0x0A, Modifiers.L_SHIFT),   // G
            0x24 to Key(0x0B, Modifiers.L_SHIFT),   // H
            0x25 to Key(0x0C, Modifiers.L_SHIFT),   // I
            0x26 to Key(0x0D, Modifiers.L_SHIFT),   // J
            0x27 to Key(0x0E, Modifiers.L_SHIFT),   // K
            0x28 to Key(0x0F, Modifiers.L_SHIFT),   // L
            0x29 to Key(0x10, Modifiers.L_SHIFT),   // M
            0x2A to Key(0x11, Modifiers.L_SHIFT),   // N
            0x2B to Key(0x12, Modifiers.L_SHIFT),   // O
            0x2C to Key(0x13, Modifiers.L_SHIFT),   // P
            0x2D to Key(0x14, Modifiers.L_SHIFT),   // Q
            0x2E to Key(0x15, Modifiers.L_SHIFT),   // R
            0x2F to Key(0x16, Modifiers.L_SHIFT),   // S
            0x30 to Key(0x17, Modifiers.L_SHIFT),   // T
            0x31 to Key(0x18, Modifiers.L_SHIFT),   // U
            0x32 to Key(0x19, Modifiers.L_SHIFT),   // V
            0x33 to Key(0x1A, Modifiers.L_SHIFT),   // W
            0x34 to Key(0x1B, Modifiers.L_SHIFT),   // X
            0x35 to Key(0x1D, Modifiers.L_SHIFT),   // Y
            0x36 to Key(0x1C, Modifiers.L_SHIFT),   // Z
            0x3D to Key(0x2B),   // Tab
            0x3E to Key(0x2C),   // Space
            0x42 to Key(0x28),   // Return
            0x43 to Key(0x2A),   // Backspace
            0x44 to Key(0x1E, Modifiers.R_ALT),   // ~
            0x45 to Key(0x38, Modifiers.L_SHIFT),   // _
            0x47 to Key(0x05, Modifiers.R_ALT),   // {
            0x48 to Key(0x11, Modifiers.R_ALT),   // }
            0x49 to Key(0x1A, Modifiers.R_ALT),   // |
            0x4A to Key(0x37, Modifiers.L_SHIFT),   // :
            0x4B to Key(0x1F, Modifiers.L_SHIFT),   // "
            0x4C to Key(0x36, Modifiers.L_SHIFT),   // ?
            0x6F to Key(0x29),   // Escape
            0x70 to Key(0x4C)    // Delete
        )

        val keyCodeToCharacterMap = mapOf(
            0x07 to "0",
            0x08 to "1",
            0x09 to "2",
            0x0A to "3",
            0x0B to "4",
            0x0C to "5",
            0x0D to "6",
            0x0E to "7",
            0x0F to "8",
            0x10 to "9",
            0x11 to "*",
            0x12 to "#",
            0x1D to "a",
            0x1E to "b",
            0x1F to "c",
            0x20 to "d",
            0x21 to "e",
            0x22 to "f",
            0x23 to "g",
            0x24 to "h",
            0x25 to "i",
            0x26 to "j",
            0x27 to "k",
            0x28 to "l",
            0x29 to "m",
            0x2A to "n",
            0x2B to "o",
            0x2C to "p",
            0x2D to "q",
            0x2E to "r",
            0x2F to "s",
            0x30 to "t",
            0x31 to "u",
            0x32 to "v",
            0x33 to "w",
            0x34 to "x",
            0x35 to "y",
            0x36 to "z",
            0x37 to ",",
            0x38 to ".",
            0x3D to " ",
            0x3E to " ",
            0x42 to "\u23CE",
            0x43 to BACKSPACE,
            0x44 to "`",
            0x45 to "-",
            0x46 to "=",
            0x47 to "[",
            0x48 to "]",
            0x49 to "\\",
            0x4A to ";",
            0x4B to "'",
            0x4C to "/",
            0x4D to "@",
            0x51 to "+",
            0x6F to "",
            0x70 to ""
        )

        val shiftKeyCodeToCharacterMap = mapOf(
            0x07 to ")",
            0x08 to "!",
            0x0B to "$",
            0x0C to "%",
            0x0D to "^",
            0x0E to "&",
            0x10 to "(",
            0x1D to "A",
            0x1E to "B",
            0x1F to "C",
            0x20 to "D",
            0x21 to "E",
            0x22 to "F",
            0x23 to "G",
            0x24 to "H",
            0x25 to "I",
            0x26 to "J",
            0x27 to "K",
            0x28 to "L",
            0x29 to "M",
            0x2A to "N",
            0x2B to "O",
            0x2C to "P",
            0x2D to "Q",
            0x2E to "R",
            0x2F to "S",
            0x30 to "T",
            0x31 to "U",
            0x32 to "V",
            0x33 to "W",
            0x34 to "X",
            0x35 to "Y",
            0x36 to "Z",
            0x37 to "<",
            0x38 to ">",
            0x3D to " ",
            0x3E to " ",
            0x42 to "\u23CE",
            0x43 to BACKSPACE,
            0x44 to "~",
            0x45 to "_",
            0x47 to "{",
            0x48 to "}",
            0x49 to "|",
            0x4A to ":",
            0x4B to "\"",
            0x4C to "?",
            0x6F to "",
            0x70 to ""
        )
    }
}