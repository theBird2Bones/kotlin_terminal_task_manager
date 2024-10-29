package tira

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory

fun main() {
//    println("Start application")//fixme: logger
    Tira
        .init()
        .run()

    // using terminal
    /*
        val tf = DefaultTerminalFactory()

        val term = tf.createTerminal()

        term.enterPrivateMode();

        val tg = term.newTextGraphics()

        tg.putString(TerminalPosition(5,5), "avada kedavra")
        term.flush()

        var keystroke = term.readInput()

        while(keystroke.keyType != KeyType.ArrowDown) {
            term.putCharacter(keystroke.character)

            term.flush()
            keystroke = term.readInput()
        }

        term.exitPrivateMode()
        term.close()
    */

//    val tf = DefaultTerminalFactory()
//    val term = tf.createTerminal()
//
//    term.putString("avada kedavra")
//
//    Thread.sleep(2000)
//
//    val screen = TerminalScreen(term) // good to go opening task info
//    screen.startScreen()
//    screen.setCharacter(5,5, TextCharacter.fromCharacter('g').first())
//    screen.se
//    screen.refresh()
//    Thread.sleep(500)
//    screen.stopScreen()
//    screen.startScreen()
//    screen.setCharacter(6,6, TextCharacter.fromCharacter('c').first())
//    screen.refresh()
//    Thread.sleep(2000)
//    screen.stopScreen()
//    Thread.sleep(2000)
//
//    term.exitPrivateMode()

}
