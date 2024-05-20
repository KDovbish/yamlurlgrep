package dk.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Сервисый класс, предоставляющий методы сопоставления с регулярными выражениями
 */
public class RegExpMatchingService {

    //  Регулярные выражения, которыми оперирует метод isUrl() для распознования того, является ли строка уролом.
    private final String[] URL_REG_EXP = {
            "^https?://[^\\s]+"  /* http://<любые символы кроме пробелов> и https://<любые символы кроме проблов> */ ,
            "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,3}(/.*)?"  /* доменное имя типа service.sense.bank.int или service.sense.bank.int/account... */,
            "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,3}:[0-9]+(/.*)?"  /* доменное имя типа service.sense.bank.int:8080 или service.sense.bank.int:8080/account... */
    };
    //  Единажды преобразованный массив в список, чтобы много раз не делать подобное преобразование в методе isUrl()
    private List<String> urlRegExpList = Arrays.asList(URL_REG_EXP);

    /**
     * Сопоставление строки со списком регулярных выражений
     * @param list список регулярных выражений
     * @param s строка для проверки
     * @return true - если хотя бы одно регулярное выражение из списка соответствует строке, false - в противном случае
     */
    public boolean withList(List<String> list, String s) {
        return list.stream().anyMatch(e -> Pattern.matches(e, s));
    }

    /**
     * Является ли строка урлом?
     * @param s строка для проверки
     * @return true(является)/false(не является)
     */
    public boolean isUrl(String s) {
        return withList(urlRegExpList, s);
    }
}
