/**
*
* @author 	MAHDI SHAHROUEI   mahdi.shahrouei@ogr.sakarya.edu.tr
* @since 	05.04.2024
* <p>
* 	projemin main sinifi ki butun fonksiyonlarimi iceriyor
* </p>
*/

package Odev_1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;
import java.text.DecimalFormat;

public class main {
    public static void main(String[] args) 
    {
        try {
        	//github repo linki almak
            Scanner scanner = new Scanner(System.in);
            System.out.print("GitHub Repository URL: ");
            String repoUrl = scanner.nextLine();
            scanner.close();

            File tmpDir = tmpDirOlustur();
            //dosya klonlamak
            System.out.println("Klonlama başlıyor...");
            KomutuCalıstır("git", "clone", repoUrl, tmpDir.getAbsolutePath());

            System.out.println("Klonlama tamamlandı.");
            
            //.java dosyalari getirmek
            Files.walk(tmpDir.toPath())
                 .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
                 .forEach(path -> {  File file = path.toFile();
                     try {
                         if (SinifliGetir(file)) 
                         {
                             analyzeJavaFile(file);
                         }
                     } catch (IOException e)
                     {
                         e.printStackTrace();
                     }
                 });

            cleanUp(tmpDir);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static File tmpDirOlustur() throws IOException {
        File tmpDir = Files.createTempDirectory("git_repo_").toFile();
        return tmpDir;
    }

    private static void KomutuCalıstır(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        process.waitFor();
    }
    //sinif olanlar getirmek 
    private static boolean SinifliGetir(File javaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(javaFile));
        String line;
        boolean sinifli = false;

        while ((line = reader.readLine()) != null) {
            if (line.contains("class "))
            {
            	sinifli = true;
                break;
            }
        }
        reader.close();

        return sinifli;
    }
    	
    
    // code satir sayisini bulmak icin fonk
    private static int kodSatiriBul(File javaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(javaFile));
        String line;
        int kodSatiri = 0;
        boolean insideComment = false;
        boolean insideJavadoc = false;

        while ((line = reader.readLine()) != null)
        {
            line = line.trim();

            if (line.isEmpty())
            { 
                continue;	 // Boş satırları atla
            }

            if (line.startsWith("/*")) {
                insideComment = true;
                if (line.startsWith("/**"))
                {
                    insideJavadoc = true;
                }
            } else if (line.endsWith("*/"))
            {
                insideComment = false;
                if (insideJavadoc) {
                    insideJavadoc = false;
                }
            } else if (insideComment) {
                if (insideJavadoc && line.startsWith("*")) {
                    // Javadoc içindeki satırları atla
                    continue;
                }
            } else if (!line.startsWith("//")) { // Tek satırlı yorumları kontrol et
            	kodSatiri++; // Kod satırı olarak kabul et
            }
        }

        reader.close();
        return kodSatiri;
    }


    private static void analyzeJavaFile(File javaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(javaFile));
        String line;
        int locSayisi = 0; // Toplam satır sayısı (LOC) için sayaç
        int fonkSayisi = 0;  // Toplam fonk sayısı için sayaç
        boolean insideMethod = false;
        boolean insideComment = false;
        boolean insideJavadoc = false;
        int javadocSatiri = 0;   // Toplam javadoc sayısı için sayaç
        int otherYorumSatiri = 0;    // Toplam diger yorumsatir sayısı için sayaç

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            locSayisi++; // Her satırı say

            if (line.isEmpty()) { // Boş satırları kontrol et
                continue; // Boş satırları atla
            }

            if (line.startsWith("/*")) 
            {
                insideComment = true;
                if (line.startsWith("/**")) 
                {
                    insideJavadoc = true;
                }
            } else if (line.endsWith("*/")) 
            {
                insideComment = false;
                if (insideJavadoc) {
                    insideJavadoc = false;
                }
            } 
            else if (insideComment) {
                if (insideJavadoc && line.startsWith("*"))
                {
                	javadocSatiri++;
                } else 		// Boş olmayan yorum satırlarını kontrol et
                { 
                	otherYorumSatiri++;
                }
            } else if (line.contains("//")) 	// Tek satırlı yorumlar
            { 
            	otherYorumSatiri++;
            }

            if (!insideMethod && (line.contains("public ") || line.contains("private ") || line.contains("protected "))) {
                if (line.contains("("))     //  method kontrolu
                { 
                	fonkSayisi++;
                    insideMethod = true;
                }
            }

            if (insideMethod && line.endsWith("}")) 	// methodun bitisini kontrol et
            { 
                insideMethod = false;
            }
        }

        reader.close();

        // Kod satırı sayısını hesaplayarak alınan değeri aktar
        int kodSatiri = kodSatiriBul(javaFile);
        
        // Yorum Sapma Yüzdesinin Hesabı

        double YG = ((javadocSatiri + otherYorumSatiri) * 0.8) / fonkSayisi;
        double YH = (kodSatiri / (double) fonkSayisi) * 0.3;

        double yorumSapma = ((100 * YG) / YH) - 100;
        DecimalFormat df = new DecimalFormat("#.##");

        
        //sonuclari ekrana yazdirmak
        System.out.println();
        System.out.println("Dosya Adı: " + javaFile.getName());
        System.out.println("Javadoc Satır Sayısı: " + javadocSatiri);
        System.out.println("Yorum Satırı Sayısı: " + otherYorumSatiri);
        System.out.println("Kod Satırı Sayısı: " + kodSatiri);
        System.out.println("LOC: " + locSayisi);
        System.out.println("Fonksiyon Sayısı: " + fonkSayisi);
        System.out.println("Yorum Sapma Yüzdesi: %" + df.format(yorumSapma));
        System.out.println("----------------------------");
    }

    //dizin (directory) ve alt dizinlerindeki tüm dosyaları ve dizinleri sil
    private static void cleanUp(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    cleanUp(file);
                }
            }
        }
        dir.delete();
    }
    
}