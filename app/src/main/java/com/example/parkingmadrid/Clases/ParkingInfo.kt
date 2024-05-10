package com.example.parkingmadrid.Clases

data class ParkingInfo(
    val address: String,
    val administrativeArea: String,
    val areaCode: String,
    val category: String,
    val country: String,
    val family: String,
    val familyCode: String,
    val id: Int,
    val latitude: String,
    val longitude: String,
    val name: String,
    val nickName: String,
    val state: String,
    val town: String,
    val type: String,
    val occupations: List<Occupation>
)

data class Occupation(
    val code: String,
    val free: Int,
    val moment: String,
    val name: String,
    val renewalIndex: String
)
