package io.webcontify.collection

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<CollectionApplication>()
        .with(DevelopmentContainersConfiguration::class)
        .run(*args)
}
