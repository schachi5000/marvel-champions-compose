package net.schacher.mcc.shared.model

data class Pack(
    val id: Int,
    val name: String,
    val code: String,
    val position: Int,
    val cards: List<Card>,
    val url: String,
) {
    val cardCodes: List<String>
        get() = cards.map { it.code }
}
