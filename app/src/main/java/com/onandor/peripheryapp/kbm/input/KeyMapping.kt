package com.onandor.peripheryapp.kbm.input

class KeyMapping {

    object ModifierKeys {
        val NONE = 0x00
        val LEFT_CTRL = 0x01
        val LEFT_SHIFT = 0x02
        val LEFT_ALT = 0x04
        val LEFT_META = 0x08
        val RIGHT_CTRL = 0x10
        val RIGHT_SHIFT = 0x20
        val RIGHT_ALT = 0x40
        val RIGHT_META = 0x80
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

        // Android key code -> HID key code
        val keyCodeToScanCode_EnUs = mapOf(
            0x1D to 0x04,   // a  A
            0x1E to 0x05,   // b  B
            0x1F to 0x06,   // c  C
            0x20 to 0x07,   // d  D
            0x21 to 0x08,   // e  E
            0x22 to 0x09,   // f  F
            0x23 to 0x0A,   // g  G
            0x24 to 0x0B,   // h  H
            0x25 to 0x0C,   // i  I
            0x26 to 0x0D,   // j  J
            0x27 to 0x0E,   // k  K
            0x28 to 0x0F,   // l  L
            0x29 to 0x10,   // m  M
            0x2A to 0x11,   // n  N
            0x2B to 0x12,   // o  O
            0x2C to 0x13,   // p  P
            0x2D to 0x14,   // q  Q
            0x2E to 0x15,   // r  R
            0x2F to 0x16,   // s  S
            0x30 to 0x17,   // t  T
            0x31 to 0x18,   // u  U
            0x32 to 0x19,   // v  V
            0x33 to 0x1A,   // w  W
            0x34 to 0x1B,   // x  X
            0x35 to 0x1C,   // y  Y
            0x36 to 0x1D,   // z  Z
            0x08 to 0x1E,   // 1  !
            0x09 to 0x1F,   // 2
            0x0A to 0x20,   // 3
            0x0B to 0x21,   // 4  $
            0x0C to 0x22,   // 5  %
            0x0D to 0x23,   // 6  ^
            0x0E to 0x24,   // 7  &
            0x0F to 0x25,   // 8
            0x10 to 0x26,   // 9  (
            0x07 to 0x27,   // 0  )
            0x42 to 0x28,   // Return
            0x6F to 0x29,   // Escape
            0x43 to 0x2A,   // Backspace
            0x3D to 0x2B,   // Tab
            0x3E to 0x2C,   // Space
            0x45 to 0x2D,   // -  _
            0x46 to 0x2E,   // =
            0x47 to 0x2F,   // [  {
            0x48 to 0x30,   // ]  }
            0x49 to 0x31,   // \  |
            0x4A to 0x33,   // ;  :
            0x4B to 0x34,   // '  "
            0x44 to 0x35,   // `  ~
            0x37 to 0x36,   // ,  <
            0x38 to 0x37,   // .  >
            0x4C to 0x38,   // /  ?
            0x70 to 0x4C    // Delete
        )

        val shiftKeyCodeToScanCode_EnUs = mapOf(
            0x4D to 0x1F,   // @
            0x12 to 0x20,   // #
            0x11 to 0x25,   // *
            0x51 to 0x2E    // +
        )

        val scanCodeToCharacter_EnUs = mapOf(
            0x04 to "a",
            0x05 to "b",
            0x06 to "c",
            0x07 to "d",
            0x08 to "e",
            0x09 to "f",
            0x0A to "g",
            0x0B to "h",
            0x0C to "i",
            0x0D to "j",
            0x0E to "k",
            0x0F to "l",
            0x10 to "m",
            0x11 to "n",
            0x12 to "o",
            0x13 to "p",
            0x14 to "q",
            0x15 to "r",
            0x16 to "s",
            0x17 to "t",
            0x18 to "u",
            0x19 to "v",
            0x1A to "w",
            0x1B to "x",
            0x1C to "y",
            0x1D to "z",
            0x1E to "1",
            0x1F to "2",
            0x20 to "3",
            0x21 to "4",
            0x22 to "5",
            0x23 to "6",
            0x24 to "7",
            0x25 to "8",
            0x26 to "9",
            0x27 to "0",
            0x28 to "\u23CE",
            0x29 to "",
            0x2A to BACKSPACE,
            0x2B to " ",
            0x2C to " ",
            0x2D to "-",
            0x2E to "=",
            0x2F to "[",
            0x30 to "]",
            0x31 to "\\",
            0x33 to ";",
            0x34 to "'",
            0x35 to "`",
            0x36 to ",",
            0x37 to ".",
            0x38 to "/",
            0x4C to ""
        )

        val shiftScanCodeToCharacter_EnUs = mapOf(
            0x04 to "A",
            0x05 to "B",
            0x06 to "C",
            0x07 to "D",
            0x08 to "E",
            0x09 to "F",
            0x0A to "G",
            0x0B to "H",
            0x0C to "I",
            0x0D to "J",
            0x0E to "K",
            0x0F to "L",
            0x10 to "M",
            0x11 to "N",
            0x12 to "O",
            0x13 to "P",
            0x14 to "Q",
            0x15 to "R",
            0x16 to "S",
            0x17 to "T",
            0x18 to "U",
            0x19 to "V",
            0x1A to "W",
            0x1B to "X",
            0x1C to "Y",
            0x1D to "Z",
            0x1E to "!",
            0x1F to "@",
            0x20 to "#",
            0x21 to "$",
            0x22 to "%",
            0x23 to "^",
            0x24 to "&",
            0x25 to "*",
            0x26 to "(",
            0x27 to ")",
            0x28 to "\u23CE",
            0x29 to "",
            0x2A to BACKSPACE,
            0x2B to " ",
            0x2C to " ",
            0x2D to "_",
            0x2E to "+",
            0x2F to "{",
            0x30 to "}",
            0x31 to "|",
            0x33 to ":",
            0x34 to "\"",
            0x35 to "~",
            0x36 to "<",
            0x37 to ">",
            0x38 to "?",
            0x4C to ""
        )
    }
}