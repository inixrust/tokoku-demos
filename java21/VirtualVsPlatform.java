import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/*
 * D2.2 (slide 17): 10.000 task.
 *
 *   java VirtualVsPlatform.java io     -> sleep 1 s per task: fixed pool 200 (±50 s) vs virtual thread (±1 s)
 *   java VirtualVsPlatform.java cpu    -> task CPU-bound: virtual thread TIDAK lebih cepat
 *
 * Momen kunci: virtual thread mempercepat I/O-bound (menunggu), bukan CPU-bound.
 */
public class VirtualVsPlatform {

	static final int TASKS = 10_000;

	public static void main(String[] args) throws Exception {
		boolean io = args.length == 0 || args[0].equals("io");
		int cpuTasks = 2_000;
		System.out.printf("Mode: %s, CPU: %d core%n", io ? "I/O (sleep 1 s)" : "CPU-bound", Runtime.getRuntime().availableProcessors());
		run("fixed pool 200 (platform)", Executors.newFixedThreadPool(200), io, io ? TASKS : cpuTasks);
		run("virtual thread per task  ", Executors.newVirtualThreadPerTaskExecutor(), io, io ? TASKS : cpuTasks);
	}

	static void run(String name, ExecutorService executor, boolean io, int tasks) throws Exception {
		long start = System.nanoTime();
		try (executor) {
			IntStream.range(0, tasks).forEach(i -> executor.submit(() -> io ? sleep() : burnCpu()));
		} // close() menunggu semua task selesai
		System.out.printf("%s: %d task dalam %.1f s%n", name, tasks, Duration.ofNanos(System.nanoTime() - start).toMillis() / 1000.0);
	}

	static Object sleep() {
		try {
			Thread.sleep(Duration.ofSeconds(1));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

	static Object burnCpu() {
		long x = 0;
		for (int i = 0; i < 20_000_000; i++) {
			x += i % 7;
		}
		return x;
	}

}
