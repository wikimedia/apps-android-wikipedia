package org.wikipedia.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.util.UiState

class HomeViewModel : ViewModel() {
    private val _sampleImagesFlow = MutableStateFlow<UiState<List<String>>>(UiState.Loading)
    val sampleImagesFlow: StateFlow<UiState<List<String>>> = _sampleImagesFlow.asStateFlow()

    init {
        _sampleImagesFlow.value = UiState.Success(listOf(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/SW_Hullathy_Gram_Panchayat_Villages_Nilgiris_Nov24_A7CR_05293.jpg/1280px-SW_Hullathy_Gram_Panchayat_Villages_Nilgiris_Nov24_A7CR_05293.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/10/Color_of_Friendship.jpg/1280px-Color_of_Friendship.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/MAP_Expo_Empereur_Ojin_Poup%C3%A9e_03_01_2012.jpg/1280px-MAP_Expo_Empereur_Ojin_Poup%C3%A9e_03_01_2012.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Sachsenheim_-_Ochsenbach_-_Geigersberg_-_n%C3%B6rdlicher_Teil_von_SSO_im_M%C3%A4rz.jpg/1280px-Sachsenheim_-_Ochsenbach_-_Geigersberg_-_n%C3%B6rdlicher_Teil_von_SSO_im_M%C3%A4rz.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Templo_de_Rams%C3%A9s_II%2C_Abu_Simbel%2C_Egipto%2C_2022-04-02%2C_DD_26-28_HDR.jpg/1280px-Templo_de_Rams%C3%A9s_II%2C_Abu_Simbel%2C_Egipto%2C_2022-04-02%2C_DD_26-28_HDR.jpg",
        ))
    }
}
