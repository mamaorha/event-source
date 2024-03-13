package co.il.mh.event.source.core

interface CommandHandler<Command, Event> {
    fun execute(commands: List<Command>): List<Event>
}