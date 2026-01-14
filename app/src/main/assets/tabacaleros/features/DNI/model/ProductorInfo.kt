package com.codeco.tabacaleros.features.DNI.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Person(
    val name: String,
    val age: Int,
    val sex: String,
    val phone: String,
    val estado: Boolean
) : Parcelable

@Parcelize
data class ProductorInfo(
    val idProductor: Int,
    val cuil: String,
    val nombre: String,
    val fechaNacimiento: String?,
    val sexo: String?,
    val estado: String,
    val urlDni: String?,
    val urlFoto: String?,
    val productorPhone: String?,
    val phoneVerification: Boolean?
) : Parcelable
