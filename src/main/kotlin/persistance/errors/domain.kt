package tira.persistance.errors

object domain {
    class TiraError(cause: Throwable, message: String) : Throwable(cause) {
        //todo: tbd
    }
}
