package pl.kijewski.synbee.data

/**
 * What the screens need from a hub: a real one over the app API ([HubClient]), or the sample home of the demo
 * ([DemoHub]). Pairing and the key check stay in [HubClient]: the demo needs neither.
 */
interface Hub {
    /** The address that answered last, to show "at home" or "away"; null for the demo. */
    val lastGood: String?
    suspend fun me(): Me
    suspend fun connection(): Connection
    suspend fun home(): Home
    suspend fun details(id: Int): ActionDetails
    suspend fun room(id: Int): RoomPage
    suspend fun run(id: Int): RunResult
}
