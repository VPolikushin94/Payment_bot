package com.bot.payment

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.extensions.filters.Filter
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.regex.Pattern
import kotlin.concurrent.fixedRateTimer

val CHAT_ID = System.getenv("org.bot.CHAT_ID")?.toLong() ?: error("No org.bot.CHAT_ID")
const val FEED_BACK_TITLE = "Тариф С моей обраткой"

val chatId = ChatId.fromId(CHAT_ID)
val dailyPayments = mutableMapOf<LocalDate, Double>()
val dailyFeedBackPayments = mutableMapOf<LocalDate, Double>()
var feedBackTariffMessagesNumber = 0
var otherTariffMessagesNumber = 0

fun main() {
    val bot = bot {
        token = System.getenv("BOT_TOKEN") ?: error("No token")
        println("Program is running")

        dispatch {
            command("сумма") {
                sendDailyReport(bot)
            }

            message(Filter.Text) {
                processMessage(message)
            }
        }
    }

    startDailyReport(bot)

    bot.startPolling()
}

fun processMessage(message: Message) {
    if (message.chat.id != CHAT_ID) return
    val text = message.text ?: return

    val today = LocalDate.now()

    // Ищем "Payment Amount" и числа
    val paymentAmountPattern = Pattern.compile("Payment Amount.*?(\\d+(\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
    val feedBackPattern = Pattern.compile(FEED_BACK_TITLE, Pattern.CASE_INSENSITIVE)

    val paymentAmountMatcher = paymentAmountPattern.matcher(text)
    val tariffMatcher = feedBackPattern.matcher(text)


    var totalPaymentAmount = 0.0
    var totalFeedBack = 0.0

    if (paymentAmountMatcher.find()) {
        val amount = paymentAmountMatcher.group(1)?.toDoubleOrNull()
        amount?.let {
            totalPaymentAmount += amount
            if (tariffMatcher.find()) {
                totalFeedBack += amount
                feedBackTariffMessagesNumber++
            }

            otherTariffMessagesNumber++
        }
    }

    if (totalPaymentAmount > 0) {
        dailyPayments[today] = dailyPayments.getOrDefault(today, 0.0) + totalPaymentAmount
        dailyFeedBackPayments[today] = dailyFeedBackPayments.getOrDefault(today, 0.0) + totalFeedBack
    }
}

fun startDailyReport(bot: Bot) {
    fixedRateTimer("dailyReportTimer", initialDelay = getInitialDelay(), period = 24 * 60 * 60 * 1000L) {
        runBlocking {
            sendDailyReport(bot)

            // Очищаем сумму на следующий день
            dailyPayments.remove(LocalDate.now())
            dailyFeedBackPayments.remove(LocalDate.now())
            otherTariffMessagesNumber = 0
            feedBackTariffMessagesNumber = 0
        }
    }
}

fun getInitialDelay(): Long {
    val now = LocalTime.now()
    val target = LocalTime.of(23, 59)
    val delaySeconds = if (now.isBefore(target)) {
        Duration.between(now, target).seconds
    } else {
        Duration.between(now, target.plusHours(24)).seconds
    }
    return delaySeconds * 1000
}

private fun sendDailyReport(bot: Bot) {
    val today = LocalDate.now()
    val dailyPayments = dailyPayments.getOrDefault(today, 0.0)
    val dailyFeedBackPayments = dailyFeedBackPayments.getOrDefault(today, 0.0)

    bot.sendMessage(
        chatId = chatId,
        text = (
                "Итог за $today\n" +
                        "Общая сумма: %.2f\n" +
                        "Кол-во: $otherTariffMessagesNumber\n\n" +
                        "Сумма с обратной связью: %.2f\n" +
                        "Кол-во с обратной связью: $feedBackTariffMessagesNumber"
                ).format(dailyPayments, dailyFeedBackPayments)
    )
}

