package com.onandor.peripheryapp.kbm.input

data class ButtonData(
    val text: String,
    val scanCode: Int
)

class ExtendedButtons {

    object Arrows {
        val LEFT = ButtonData("", KeyMapping.ArrowKeys.LEFT,)
        val RIGHT = ButtonData("", KeyMapping.ArrowKeys.RIGHT)
        val UP = ButtonData("", KeyMapping.ArrowKeys.UP)
        val DOWN = ButtonData("", KeyMapping.ArrowKeys.DOWN)
    }

    object Modifiers {
        val L_CTRL = ButtonData("L Ctrl", KeyMapping.ModifierKeys.L_CTRL)
        val R_CTRL = ButtonData("R Ctrl", KeyMapping.ModifierKeys.R_CTRL)
        val SHIFT = ButtonData("Shift", KeyMapping.ModifierKeys.L_SHIFT)
        val L_META = ButtonData("L Win", KeyMapping.ModifierKeys.L_META)
        val L_ALT = ButtonData("L Alt", KeyMapping.ModifierKeys.L_ALT)
        val R_ALT = ButtonData("R Alt", KeyMapping.ModifierKeys.R_ALT)
        val R_META = ButtonData("R Win", KeyMapping.ModifierKeys.R_META)
    }

    object FRow {
        val F1 = ButtonData("F1", KeyMapping.FRow.F1)
        val F2 = ButtonData("F2", KeyMapping.FRow.F2)
        val F3 = ButtonData("F3", KeyMapping.FRow.F3)
        val F4 = ButtonData("F4", KeyMapping.FRow.F4)
        val F5 = ButtonData("F5", KeyMapping.FRow.F5)
        val F6 = ButtonData("F6", KeyMapping.FRow.F6)
        val F7 = ButtonData("F7", KeyMapping.FRow.F7)
        val F8 = ButtonData("F8", KeyMapping.FRow.F8)
        val F9 = ButtonData("F9", KeyMapping.FRow.F9)
        val F10 = ButtonData("F10", KeyMapping.FRow.F10)
        val F11 = ButtonData("F11", KeyMapping.FRow.F11)
        val F12 = ButtonData("F12", KeyMapping.FRow.F12)
    }

    object SpecialKeys {
        val ESCAPE = ButtonData("Esc", KeyMapping.SpecialKeys.ESCAPE)
        val TAB = ButtonData("Tab", KeyMapping.SpecialKeys.TAB)
        val BACKSPACE = ButtonData("Bksp", KeyMapping.SpecialKeys.BACKSPACE)
        val PRINT_SCR = ButtonData("PrSc", KeyMapping.SpecialKeys.PRINT_SCR)
        val SCRLK = ButtonData("ScrLk", KeyMapping.SpecialKeys.SCRLK)
        val PAUSE = ButtonData("Pause", KeyMapping.SpecialKeys.PAUSE)
        val INSERT = ButtonData("Ins", KeyMapping.SpecialKeys.INSERT)
        val HOME = ButtonData("Home", KeyMapping.SpecialKeys.HOME)
        val PAGE_UP = ButtonData("Pgup", KeyMapping.SpecialKeys.PAGE_UP)
        val DELETE = ButtonData("Del", KeyMapping.SpecialKeys.DELETE)
        val END = ButtonData("End", KeyMapping.SpecialKeys.END)
        val PAGE_DOWN = ButtonData("Pgdn", KeyMapping.SpecialKeys.PAGE_DOWN)
    }
}