package com.vermont.possin

import android.os.Parcel
import android.os.Parcelable

data class Cryptocurrency(val name: String, val shortname: String, val chain: String, val logo: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(shortname)
        parcel.writeString(chain)
        parcel.writeString(logo)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Cryptocurrency> {
        override fun createFromParcel(parcel: Parcel): Cryptocurrency {
            return Cryptocurrency(parcel)
        }

        override fun newArray(size: Int): Array<Cryptocurrency?> {
            return arrayOfNulls(size)
        }
    }
}
