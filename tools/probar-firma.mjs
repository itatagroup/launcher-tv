// Prueba en el PC la verificación de firma del actualizador, con el mismo código Java que va en la app.
// Uso: node tools/probar-firma.mjs   (necesita javac y java en el PATH)
//
// Firma un manifiesto con una clave de PRUEBA que se crea en memoria (nunca la de keystore/), compila
// app/src/main/java/cl/itatagroup/launcher/FirmaManifiesto.java tal cual (no usa APIs de Android) junto con una
// clase mínima que llama a verificarFirma, y comprueba: firma buena → true; un byte cambiado → false.
import { execFileSync } from "node:child_process";
import { generateKeyPairSync, sign } from "node:crypto";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const fuente = join(raiz, "app", "src", "main", "java", "cl", "itatagroup", "launcher", "FirmaManifiesto.java");
const tmp = mkdtempSync(join(tmpdir(), "probar-firma-"));

const PRUEBA = `import cl.itatagroup.launcher.FirmaManifiesto;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PruebaFirma {
    public static void main(String[] a) throws Exception {
        byte[] datos = Files.readAllBytes(Paths.get(a[0]));
        String firma = new String(Files.readAllBytes(Paths.get(a[1])), "UTF-8");
        String clave = new String(Files.readAllBytes(Paths.get(a[2])), "UTF-8");
        System.out.print(FirmaManifiesto.verificarFirma(datos, firma, clave));
    }
}
`;

let fallas = 0;
try {
  writeFileSync(join(tmp, "PruebaFirma.java"), PRUEBA);
  execFileSync("javac", ["--release", "8", "-Xlint:-options", "-d", join(tmp, "clases"), fuente, join(tmp, "PruebaFirma.java")], { stdio: "inherit" });

  const { privateKey, publicKey } = generateKeyPairSync("ec", { namedCurve: "P-256" });
  const otra = generateKeyPairSync("ec", { namedCurve: "P-256" }).publicKey;
  const b64 = (k) => k.export({ type: "spki", format: "der" }).toString("base64");
  const manifiesto = Buffer.from(JSON.stringify({
    formato: 1,
    repo: "itatagroup/launcher-tv",
    apps: { "launcher-android": { version: "9.9.9", versionCode: 99, url: "https://github.com/itatagroup/launcher-tv/releases/download/v9.9.9/inicio-9.9.9.apk", tamano: 1, sha256: "0".repeat(64), notas: "prueba con ñ y tildes: áéíóú" } },
  }, null, 2));
  const firma = sign("sha256", manifiesto, privateKey).toString("base64");

  const alterado = Buffer.from(manifiesto);
  alterado[alterado.length - 5] ^= 0x01;
  const firmaAlterada = Buffer.from(firma, "base64");
  firmaAlterada[firmaAlterada.length - 1] ^= 0x01;

  const casos = [
    ["firma buena", manifiesto, firma, b64(publicKey), "true"],
    ["un byte del manifiesto cambiado", alterado, firma, b64(publicKey), "false"],
    ["un byte de la firma cambiado", manifiesto, firmaAlterada.toString("base64"), b64(publicKey), "false"],
    ["otra clave pública", manifiesto, firma, b64(otra), "false"],
    ["firma vacía", manifiesto, "", b64(publicKey), "false"],
    ["firma que no es base64", manifiesto, "esto no es base64!", b64(publicKey), "false"],
  ];
  for (const [nombre, datos, sig, clave, esperado] of casos) {
    writeFileSync(join(tmp, "datos.bin"), datos);
    writeFileSync(join(tmp, "firma.txt"), sig);
    writeFileSync(join(tmp, "clave.txt"), clave);
    const r = execFileSync("java", ["-cp", join(tmp, "clases"), "PruebaFirma", join(tmp, "datos.bin"), join(tmp, "firma.txt"), join(tmp, "clave.txt")], { encoding: "utf8" }).trim();
    const ok = r === esperado;
    if (!ok) fallas++;
    console.log(`${ok ? "OK   " : "FALLA"} ${nombre}: ${r} (se esperaba ${esperado})`);
  }
} finally {
  rmSync(tmp, { recursive: true, force: true });
}
console.log(fallas ? `\n${fallas} caso(s) fallaron` : "\nTodo en verde");
process.exit(fallas ? 1 : 0);
