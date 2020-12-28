package com.example.kmmsharedmodule

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}