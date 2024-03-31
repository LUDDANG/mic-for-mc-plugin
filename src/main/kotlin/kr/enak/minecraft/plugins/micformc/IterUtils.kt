package kr.enak.minecraft.plugins.micformc


fun <T> Iterable<T>.forEachTry(function: (T) -> Unit) {
    this.forEach {
        try {
            function(it)
        } catch (ignore: Throwable) {}
    }
}
