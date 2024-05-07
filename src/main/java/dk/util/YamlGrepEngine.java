package dk.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;
import lombok.Setter;
import lombok.Getter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Движок для поиска в yaml-файле урлов соответствующих заданным регулярным выражениям.
 * <br><br>
 * Ключевые моменты: <br>
 * Движок оперирует понятием "уровни yaml-файла". Примеры уровней: "g1cfrntapb05", "g1cfrntapb05/SMECredit", "g1cfrntapb05/SMECredit/configuration".<br>
 * Аксиомой для данного движка является следующее: в состав уровней могут входить только ключи, содержащие, в качестве значения, карту. <br>
 * Цели пользователя - это те же уровни yaml-файла, которые движок должен сначала найти, а потом проанализировать все урлы ниже.<br>
 * <br>
 * medusa по каким-то причинам(подозреваю, что из-за массивов) некоторые поля представляет с фиксированным именем json-object, в качестве значения которых
 * используется стандартный json. Приходиться парсить и его. К счастью, парсер jackson умеет представлять json картой, полностью аналогичной
 * по структуре карте, которую делает snakeyaml. Поэтому, код не сильно изменился для обработки полей json-object: добавился метод json_to_map() и
 * код по распознаванию в имени ключа строки "json-object".
 */
public class YamlGrepEngine {

    private TypeRecognitionService typeRecognitionService =  new TypeRecognitionService();
    private YamlLevels yamlLevels = new YamlLevels();
    private RegExpMatchingService regExpMatchingService = new RegExpMatchingService();

    //  Карта yaml-файла полученная через использование в конструкторе библиотеки snakeYaml
    private Map<String, Object> yamlMap;
    //  Список целей пользователя
    private List<String> targetList;
    //  Список регулярных выражений для поиска нужных пользователю урлов
    private List<String> regexpList;
    //  Дескриптор результирующего файла
    private PrintWriter resultFile;

    //  Счетчик оставшихся необработанных целей пользователя
    private Integer targetCounter;

    //  Тип сопоставления строковых значений параметров со списком регулярных выражений.
    //  Задается пользователем.
    //  По умолчанию - сопоставление будет успешным, если любое из регулярных выражений соответствует строке
    @Setter
    @Getter
    private RegExpMatchingType matchingType = RegExpMatchingType.OR;


    /**
     * Конструктор
     * @param yamlFileName Входной yaml-файл, сформированный medusa
     * @param targetList Перечень целей в yaml-файле, которые должен обработать этот движок
     * @param regexpList Список регулярных выражений, которые будут использоваться для поиска нужных пользователю урлов
     * @param resultFile Выходной файл с результатами поиска
     * @throws FileNotFoundException FileNotFoundException
     */
    public YamlGrepEngine(String yamlFileName, List<String> targetList, List<String> regexpList, PrintWriter resultFile) throws FileNotFoundException {
        this.targetList = targetList;
        this.regexpList = regexpList;
        this.resultFile = resultFile;
        yamlMap = new Yaml().load(new FileInputStream(yamlFileName));
    }

    /**
     * Запуск движка
     * @throws JsonProcessingException JsonProcessingException
     */
    public void run() throws JsonProcessingException {
        //  Количество необработанных еще целей пользователя - это весь список
        targetCounter = targetList.size();
        //  Обходим yaml-дерево...
        yamlTreeTraversal(yamlMap);
    }

    /**
     * Рекурсивный обход yaml-дерева.
     * <br><br>
     * Данный метод - это основная точка входа в функционал обхода yaml-дерева. На каждой итерации идет проверка на
     * соответствие текущего уровня yaml-файла с любой из целей пользователя. Если совпадение найдено, то все урлы ниже этого
     * уровня будет проанализироавны методом mapAnalysis().<br>
     * В этом суть - метод yamlTreeTraversal() обходит дерево и ищет цели. Метод mapAnalysis() ищет заданные пользователем урлы.<br>
     * В методе также ведется учет обработанных целей пользователя. Как только все цели пользователя будут обработаны, цикл обхода дерева прервется.
     * @param map Карта, описывающая yaml-дерево
     * @throws JsonProcessingException Генерируемое методом исключение, в случае проблем с парсиногом json
     */
    void yamlTreeTraversal(Map<String, Object> map) throws JsonProcessingException {

        //  Получить список ключей
        Set<String> keys = map.keySet();

        //  Перебираем по очереди все ключи
        Iterator<String> keysIterator = keys.iterator();
        String key;
        Object value;
        while (keysIterator.hasNext()) {
            //  Получаем текущий ключ
            key = keysIterator.next();
            //  Получаем значение ключа
            value = map.get(key);

            if (typeRecognitionService.isMap(value)) {
                //  Значение ключа является картой...

                //  Запоминаем текущий рассматриваемый уровень yaml-дерева
                yamlLevels.add(key);
                if ( targetList.stream().anyMatch(e -> e.equals(yamlLevels.getPath())) ) {
                    //  Одна из целей пользователя совпадает с текущим рассматриваемым уровнем yaml-дерева.
                    //  Делаем анализ ключей...
                    mapAnalysis((Map<String, Object>) value);

                    //  Очередная из целей обработана. Счетчик уменьшаем...
                    //  Если все цели обработаны, больше нет необходиомсти обходить дерево. Завершаем метод.
                    if (--targetCounter == 0) return;
                } else {
                    //  Текущий рассматриваемый уровень yaml-дерева не соответствует ни одной из целей пользователя.
                    //  Идем дальше вглубь дерева...
                    yamlTreeTraversal((Map<String, Object>) value);
                }
                //  Текущий ключ полностью обработан. Удалаяем его из уровня
                yamlLevels.remove();
            }

        }

    }


    /**
     * Поиск в карте урлов, заданных пользоваталем.
     * <br><br>
     * Результаты анализа урлов отображаются в выходной файле(задается через {@link #YamlGrepEngine(String, List, List, PrintWriter) конструктор})
     * @param map Карта для анализа
     * @throws JsonProcessingException Генерируемое методом исключение, в случае проблем с парсиногом json
     */
    void mapAnalysis(Map<String, Object> map) throws JsonProcessingException {

        //  Получить список ключей
        Set<String> keys = map.keySet();
        //  Перебираем по очереди все ключи
        Iterator<String> keysIterator = keys.iterator();
        String key;
        Object value;
        while (keysIterator.hasNext()) {
            //  Получаем текущий ключ
            key = keysIterator.next();
            //  Получаем значение ключа
            value = map.get(key);


            //  Исправляем ошибки парсинга medusa
            //  Распознаем ключ с именем json-object и разбираем его значение как json в карту
            //  Анализируем полученную карту через рекурсивный вызов
            if (key.equals("json-object")) {
                mapAnalysis( json_as_map((String) value) );
                continue;
            }


            if (typeRecognitionService.isMap(value)) {
                //  Если значение является картой...то идем на рекурсию
                mapAnalysis((Map<String, Object>) value);
                continue;
            }

            if (typeRecognitionService.isString(value)) {

                //  Если значение является строкой...
                //  то нужно сопостависть эту строку со списком регулярных выражений, который задал пользователь.
                //  Но это сопоставление мы будет делать только в том случае, если значение ключа является урлом!
                if (regExpMatchingService.isUrl((String) value)) {
                    //  Значение ключа является урлом! Запускаем сопоставление урла со списком регулярных выражений пользователя...
                    if (regExpMatchingService.withList(regexpList, (String) value)) {
                        //  Итак, урл в одном из параметров yaml-файла совпал с одним из регулярных выражений, заданных пользователем.
                        //  Если пользователь просит отобразить подобный случай в результирующем файле(тип сопоставления = OR)...
                        if (this.matchingType == RegExpMatchingType.OR) {
                            resultFile.println(yamlLevels.getPath() + "\t" + key + "\t" + value);
                        }
                    } else {
                        //  Урл в одном из параметров yaml-файла не сопоставляется ни с одним из реуглярных выражений, заданных пользователем.
                        //  Если пользователь желает отобразить подобный случай в результирующем файле(тип сопоставления = NOT)...
                        if (this.matchingType == RegExpMatchingType.NOT) {
                            resultFile.println(yamlLevels.getPath() + "\t" + key + "\t" + value);
                        }
                    }
                }
            }
        }

    }


    /**
     * Разобрать json и представить его как карту
     * @param json Строка с json
     * @return Карта с разобранным json
     * @throws JsonProcessingException Генерируемое методом исключение, в случае проблем с парсиногом json
     */
    Map<String, Object> json_as_map(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json.replace('\n', ' ').replace('\r', ' '), Map.class);
    }


}
