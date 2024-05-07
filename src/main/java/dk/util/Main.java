package dk.util;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.stream.Collectors;

public class Main {
    /*
    * yamlurlgrep <тип сопоставления NOT/OR> <входной yaml-файл> <файл с целями пользователя> <файл с регулярными выражениями> [файл с результатами]
    */
    public static void main(String[] args) throws FileNotFoundException, JsonProcessingException {

        if ( !(args.length == 4 || args.length == 5) ) {
            System.out.println("Аргументы модуля:");
            System.out.println("1.Тип сопоставления: OR|NOT");
            System.out.println("2.yaml-файл");
            System.out.println("3.Конфигурационный файл: цели пользователя");
            System.out.println("4.Конфигурационный файл: регулярные выражения");
            System.out.println("5.[Имя файла для результатов]");
            System.out.println();
            System.out.println("Логика работы модуля:");
            System.out.println("Осуществляется обход yaml-файла в поисках целей(3), заданных пользователем. Если цель найдена, то ниже по дереву под этой целью ищутся урлы,");
            System.out.println("соответствующие регулярным выражениям(4). В файл результатов найденные урлы выводятся в соответствии с типом сопоставления(1).");
            System.out.println("Тип сопоставления OR: урл выводиться в файл результата, если он соответствуют любому из регулярных выражений из списка(4).");
            System.out.println("Тип сопоставления NOT: урл выводиться в файл результата, если он не соответствуют ни одному из регулярных выражений из списка(4).");
            System.out.println("Цель(3) - это весь или частичный путь к секрету; В качестве разделителей пути должны использоваться слеши.");
            System.out.println("В конфигурационных файлах(3,4) может использовать символ # в качестве комментария.");
            System.out.println("Имя файла для результатов(5) является опциональным. Если оно не задано, имя файла формируется автоматически на основе имени yaml-файла(2).");
            return;
        }

        PrintWriter resultFile = null;

        if (args.length == 5) {
            resultFile = new PrintWriter(new FileOutputStream(args[4], true /* append */));
        }
        if (args.length == 4) {
            resultFile = new PrintWriter(args[1] + ".result.txt");
        }

        if (resultFile != null) {

            YamlGrepEngine yamlEngine = new YamlGrepEngine(
                    args[1], /* имя файла анализируемого yaml-файла */
                    new BufferedReaderFileWithComments(args[2]).lines().collect(Collectors.toList()), /* список целей пользователя */
                    new BufferedReaderFileWithComments(args[3]).lines().collect(Collectors.toList()), /* список регулярных выражений */
                    resultFile /* результирующий файл */
            );
            yamlEngine.setMatchingType(RegExpMatchingType.valueOf(args[0]));
            yamlEngine.run();

            resultFile.close();
        }

    }
}