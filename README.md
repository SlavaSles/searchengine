# SearchEngine
The program that indexes sites from a list in a configuration file, a certain page of a site, stops indexing,
gets statistics about indexed sites and performs a search on indexed site.

# Краткое описание
Программа выполняет следующие действия:
* индексирует сайты из списка, указанного в файле конфигурации application.yaml;
* индексирует (или индексирует повторно) определенные страницы с сайтов, указанных в файле конфигурации;
* прерывает процесс индексации в случае ошибки или по команде пользователя;
* выдает статистику по индексации сайтов, указанных в файле конфигурации;
* осуществляет поиск слов на проиндексированных сайтах.

# Используемый стек:
* JDK 17;
* Spring Boot (Web, Data JPA);
* Lombok;
* MySQL;
* Jsoup;
* Lucene Morphology;
* Maven;
* Swagger

# Инструкция по локальному запуску

1. Для работы приложения необходимо создать в базе данных MySQL, установленной локально, новую схему (по умолчанию в 
application.yaml указана "search_engine") с кодировкой utf8mb4 и прописать доступ к ней (порт, название, логин и пароль 
для доступа) в файле конфигурации application.yaml:  
url: jdbc:mysql://localhost:3306/search_engine?useSSL=false&requireSSL=false&allowPublicKeyRetrieval=true # порт и название схемы  
username: admin # логин  
password: nimad # пароль

Во время работы приложения в базе данных MySQL создаются следующие таблицы:
* site - для сохранения данных о состоянии индексации сайтов;
* page - для сохранения всех страниц сайта с контентом;
* lemma - для сохранения базовых форм слов, встречающихся на страницах сайтов;
* search_index - для сохранения информации о количестве упоминаний лемм на отдельных страницах сайтов

2. Обновить зависимости в pom.xml. Если возникнут проблемы с загрузкой библиотек Лемматизатора, то можно добавить их 
в проект вручную, скачав и собрав из исходников, расположенных в репозитории:  
<https://github.com/akuznetsov/russianmorphology>

3. После конфигурации БД можно запускать приложение, которое будет доступно на порту 8080.  

# Описание UI

UI приложения состоит из следующих вкладок:

## Вкладка Dashboard

![img.png](/docs/dashboard_tab.png "Изображение вкладки Dashboard")

Вкладка, отображаемая по умолчанию, содержит общую и индивидуальную статистику по индексируемым сайтам и их статус.  

## Вкладка Management

![img.png](/docs/management_tab.png "Изображение вкладки Management")

![img.png](/docs/stop_indexing.png "Изображение кнопки STOP INDEXING")

![img.png](/docs/add_page.png "Изображение добавления страницы в индекс")

Вкладка, на которой находятся инструменты управления индексацией:
* запуск (перезапуск) индексации всех страниц из файла конфигурации, выполняемый кнопкой "START INDEXING";
* прерывание индексации, выполняемое кнопкой "STOP INDEXING", которая появляется после нажатия кнопкой "START 
INDEXING". После успешного выполнения индексации сайтов необходимо также нажать на кнопку "STOP INDEXING" для ее
корректного завершения;
* добавление (обновление) в индексе отдельной страницы по ссылке, введенной в поле "Add/update page", выполняемое 
нажатием кнопки "ADD/UPDATE" 

## Вкладка Search

![img.png](/docs/search_tab.png "Изображение вкладки Search")

![img.png](/docs/choose_site.png "Изображение выбора сайта для поиска")

![img.png](/docs/search_result.png "Изображение результатов поиска")

Вкладка, предназначенная для осуществления поиска слов (поисковых запросов) на проиндексированных сайтах. В выпадающем 
списке можно выбрать для поиска либо все сайты из файла конфигураций либо какой-либо из них в отдельности. После нажатия 
на кнопку "Search" выполняется поиск, результаты которого отображаются под полем поискового запроса. Для выполнения 
поиска необходимо предварительно выполнить индексацию сайтов.

# Спецификация API

Спецификация API приложения доступна в OpenAPI Specification (OAS) после запуска приложения в веб-браузере по адресу:  
<http://localhost:8080/swagger-ui/index.html#/>