/*
 * D2.1 (slide 16): sealed interface + switch pattern matching tanpa default.
 *
 *   java PaymentResults.java          -> jalan, mencetak tiga hasil
 *
 * Momen kunci: hapus komentar pada baris "record Refunded" di bawah (dan tambahkan ke permits).
 * Jalankan lagi: COMPILE ERROR "the switch expression does not cover all possible input values".
 * Kompiler menunjukkan setiap switch yang harus diperbarui, bukan bug di produksi.
 */
public class PaymentResults {

	sealed interface PaymentResult permits Approved, Declined, Pending /* , Refunded */ {
	}

	record Approved(String authCode) implements PaymentResult {
	}

	record Declined(String reason) implements PaymentResult {
	}

	record Pending(int retryAfterSeconds) implements PaymentResult {
	}

	// record Refunded(long amount) implements PaymentResult {
	// }

	static String describe(PaymentResult result) {
		return switch (result) { // tanpa default: kompiler memeriksa kelengkapan
			case Approved a -> "disetujui, kode " + a.authCode();
			case Declined d -> "ditolak: " + d.reason();
			case Pending p -> "menunggu, coba lagi dalam " + p.retryAfterSeconds() + " s";
		};
	}

	public static void main(String[] args) {
		for (PaymentResult r : new PaymentResult[] { new Approved("A123"), new Declined("saldo kurang"),
				new Pending(30) }) {
			System.out.println(describe(r));
		}
	}

}
