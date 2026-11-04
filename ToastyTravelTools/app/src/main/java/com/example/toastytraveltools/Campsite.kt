package com.example.toastytraveltools

data class Campsite (
    val Name : String,
    val Description : String,
    val Location : String,
    val OccupancyLimit : Int,
    val ImagesUrl: String, // should contain an img url that can further be stored in firebase, ill try to figure out how it works exactly soon
    val Rating: Double
)