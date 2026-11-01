import java.util.ArrayList;
import java.util.List;

/*
 * D13.2 (slide 118): heap vs memory limit container. Menahan 10 MiB per 100 ms sampai habis.
 * Jalankan lewat scripts/d13.2-oom.ps1:
 *   a. -Xmx1g di limit 1 GiB         -> kernel membunuh container: OOMKilled=true, exit 137, tanpa pesan Java
 *   b. MaxRAMPercentage=70 + ExitOnOutOfMemoryError -> JVM keluar bersih dengan pesan OutOfMemoryError, exit 3
 */
public class MemoryHog {

	public static void main(String[] args) throws InterruptedException {
		System.out.printf("MaxHeap = %d MiB%n", Runtime.getRuntime().maxMemory() / (1024 * 1024));
		List<byte[]> retained = new ArrayList<>();
		while (true) {
			byte[] chunk = new byte[10 * 1024 * 1024];
			for (int i = 0; i < chunk.length; i += 4096) {
				chunk[i] = 1; // sentuh setiap page agar benar-benar menjadi RSS
			}
			retained.add(chunk);
			if (retained.size() % 10 == 0) {
				System.out.printf("ditahan %d MiB%n", retained.size() * 10);
			}
			Thread.sleep(100);
		}
	}

}
