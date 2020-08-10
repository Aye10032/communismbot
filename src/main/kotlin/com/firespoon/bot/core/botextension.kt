package com.firespoon.bot.core

import com.firespoon.bot.command.Command
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.time
import java.util.*
import kotlin.collections.HashMap

typealias ListenerRegisterer<E> = Bot.(
        EventPriority,
        suspend E.(E) -> Unit
) -> Listener<E>

typealias CommandRegisterer<E> = Bot.(
        Command<E>
) -> Unit

private val metaInfoOfAllBots = HashMap<Bot, FSBotMetaInfo>()

val Bot.listeners: HashMap<String, Listener<*>>
    get() {
        val metaInfo = metaInfoOfAllBots[this]
        if (metaInfo != null) {
            return metaInfo.listeners
        }
        throw Exception("尚未初始化")
    }

val Bot.bootTime: Int
    get() {
        val metaInfo = metaInfoOfAllBots[this]
        if (metaInfo != null) {
            return metaInfo.bootTime
        }
        throw Exception("尚未初始化")
    }

suspend fun Bot.boot() {
    //alsoLogin()
    login()
    val bootTime = (Calendar.getInstance().timeInMillis / 1000).toInt()
    val listeners = HashMap<String, Listener<*>>()
    val metaInfo = FSBotMetaInfo(bootTime, listeners)
    metaInfoOfAllBots[this] = metaInfo
}

inline fun <reified E : Event>
        Bot.registerListener(
        priority: EventPriority = EventPriority.NORMAL,
        name: String,
        noinline callback: suspend Bot.(E) -> Unit,
        registerer: ListenerRegisterer<E>
) {
    if (listeners.containsKey(name)) {
        throw Exception("Listener[$name] is already existed.")
    }

    val handler = registerer(priority) { event ->
        this@registerListener.callback(event)
    }
    listeners[name] = handler
}

fun Bot.closeListener(name: String) {
    listeners[name]?.complete()
    listeners.remove(name)
}

inline fun <reified E : Event>
        Bot.registerListenerAlways(
        priority: EventPriority = EventPriority.NORMAL,
        name: String,
        noinline callback: suspend Bot.(E) -> Unit
) {
    registerListener(priority, name, callback, Bot::_subscribeAlways)
}

inline fun <reified E : Event>
        Bot.registerListenerOnce(
        priority: EventPriority = EventPriority.NORMAL,
        name: String,
        noinline callback: suspend Bot.(E) -> Unit
) {
    registerListener(priority, name, callback, Bot::_subscribeAlways)
}

inline fun <reified E : MessageEvent>
        Bot.registerCommand(
        command: Command<E>,
        registerer: CommandRegisterer<E>
) {
    this.registerer(command)
}

inline fun <reified E : MessageEvent>
        Bot.registerCommandByListener(
        command: Command<E>,
        registerer: ListenerRegisterer<E>
) {
    registerListener(
            name = command.name,
            callback = { event ->
                val messageTime = event.message.time
                if (messageTime >= bootTime) {
                    val commandBody = command.builder(event)
                    commandBody?.(command.action)()
                }
            },
            registerer = registerer
    )
}

inline fun <reified E : MessageEvent>
        Bot.registerCommandAlways(
        command: Command<E>
) {
    registerCommandByListener(command, Bot::_subscribeAlways)
}

inline fun <reified E : MessageEvent>
        Bot.registerCommandOnce(
        command: Command<E>
) {
    registerCommandByListener(command, Bot::_subscribeOnce)
}


inline fun <reified E : Event>
        Bot._subscribeAlways(
        priority: EventPriority,
        noinline handler: suspend E.(E) -> Unit)
        : Listener<E> = subscribeAlways(priority = priority)
{ event ->
    handler(event)
}

inline fun <reified E : Event>
        Bot._subscribeOnce(
        priority: EventPriority,
        noinline handler: suspend E.(E) -> Unit)
        : Listener<E> = subscribeOnce(priority = priority)
{ event ->
    handler(event)
}