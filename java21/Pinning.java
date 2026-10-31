import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/*
 * D2.3 (slide 18): synchronized + blocking di virtual thread.
 *
 *   JDK 21:  java -XX:StartFlightRecording=filename=pin21.jfr Pinning.java
 *            jfr print --events jdk.VirtualThreadPinned pin21.jfr     -> event muncul (threshold 20 ms)
 *   JDK 25:  & "$env:JDK25_HOME\bin\java" -XX:StartFlightRecording=filename=pin25.jfr Pinning.java
 *            & "$env:JDK25_HOME\bin\jfr" print --events jdk.VirtualThreadPinned pin25.jfr  -> tidak ada (JEP 491)
 *
 * Waktu total: tambahkan -Djdk.virtualThreadScheduler.parallelism=2 (carrier sedikit, seperti container 2 CPU).
 * JDK 21: carrier ter-pin selama sleep, 50 task antre (±2,5 s). JDK 25: carrier dilepas saat sleep (±0,1 s).
 */
public class Pinning {

	public static void main(String[] args) {
		System.out.println("JDK " + Runtime.version() + ", carrier = " + Runtime.getRuntime().availableProcessors());
		long start = System.nanoTime();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			IntStream.range(0, 50).forEach(i -> executor.submit(Pinning::work));
		}
		System.out.printf("50 task selesai dalam %.1f s%n", (System.nanoTime() - start) / 1e9);
	}

	static void work() {
		// Lock milik task sendiri: tidak ada contention, fokus demo hanya pinning.
		Object local = new Object();
		synchronized (local) {
			try {
				Thread.sleep(Duration.ofMillis(100)); // blocking di dalam synchronized -> pin carrier (JDK 21)
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
