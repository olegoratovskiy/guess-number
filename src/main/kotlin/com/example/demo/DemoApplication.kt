package com.example.demo

import com.example.demo.data.GUESSED_NUMBER
import com.example.demo.data.RESULT
import com.example.demo.dsl.eq
import com.example.demo.dsl.insertInto
import com.example.demo.dsl.select
import com.example.demo.dsl.update
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import java.util.*

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Service
class GameService(val db: JdbcTemplate) {
    private fun getRandomNumber() = (1..10).shuffled().last()

    private fun getScore(id: String): Int? {
        val selectScoreResult = select().from(RESULT).where(RESULT.ID.eq(id)).fetch(db) { response, _ ->
            response.getInt("score")
        }
        return if (selectScoreResult.isNotEmpty()) {
            selectScoreResult.first()
        } else {
            null
        }
    }

    fun getScoreMessage(id: String): String {
        val score = getScore(id)
        return if (score == null) {
            "There is no user with such id"
        } else {
            "Your score is $score"
        }
    }

    fun register(): String {
        var id = UUID.randomUUID().toString()
        while (select().from(RESULT).where(RESULT.ID.eq(id)).fetch(db) { response, _ -> response }.isNotEmpty()) {
            id = UUID.randomUUID().toString()
        }
        val numberToGuess = getRandomNumber()
        insertInto(GUESSED_NUMBER, GUESSED_NUMBER.ID, GUESSED_NUMBER.SECRET_NUMBER).values(id, numberToGuess)
            .execute(db)
        insertInto(RESULT, RESULT.ID, RESULT.SCORE).values(id, 0).execute(db)
        return "Your id is $id"
    }

    fun guess(id: String, number: Int): String {
        val selectSecretNumberResult = select().from(GUESSED_NUMBER).where(GUESSED_NUMBER.ID.eq(id))
            .fetch(db) { response, _ -> response.getInt("secret_number") }
        if (selectSecretNumberResult.isEmpty()) {
            return "There is no user with such id"
        }
        if (selectSecretNumberResult.first() == number) {
            val newSecretNumber = getRandomNumber()
            update(GUESSED_NUMBER).set(GUESSED_NUMBER.SECRET_NUMBER, newSecretNumber).where(GUESSED_NUMBER.ID.eq(id))
                .execute(db)
            val score = getScore(id)
            if (score != null) {
                update(RESULT).set(RESULT.SCORE, score + 1).where(RESULT.ID.eq(id)).execute(db)
            } else {
                println("Error: Existing user with id $id doesn't have score.")
            }
            return "You have guessed! Let's play again"
        }
        return "Wrong attempt. Try again"
    }
}

@RestController
class MessageController(val service: GameService) {
    @GetMapping("/get_score")
    fun requestUserScore(@RequestParam("id") id: String) = service.getScoreMessage(id)

    @GetMapping("/register")
    fun registerUser() = service.register()

    @GetMapping("/guess")
    fun guessNumber(@RequestParam("id") id: String, @RequestParam("number") number: Int) = service.guess(id, number)
}
