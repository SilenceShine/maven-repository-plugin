# maven-repository-plugin
maven自定义仓库插件

## maven-repository-github-plugin
自定义 github为仓库的插件

### settings.xml

```xml
<servers>
    <server>
        <id>io.github.SilenceShine</id>
        <password>GITHUB TOKEN</password>
    </server>
</servers>
```

###  pom.xml

```xml
<distributionManagement>
    <repository>
        <id>Local</id>
        <name>Local Repository</name>
        <url>file://${project.build.directory}/mvn-repo</url>
    </repository>
</distributionManagement>
```

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.SilenceShine</groupId>
            <artifactId>maven-repository-github-plugin</artifactId>
            <version>${maven-repository-github-plugin.version}</version>
            <configuration>
                <!--     token 存在 及不会从setting.xml 获取password       -->
                <id>io.github.SilenceShine</id>
                <token>GITHUB TOKEN</token>
                <owner>SilenceShine</owner>
                <repository>maven-repository</repository>
                <branchRelease>release</branchRelease>
                <branchSnapshot>snapshot</branchSnapshot>
                <message>init maven-repository-github-plugin 0.0.1</message>
            </configuration>
            <executions>
                <execution>    
                    <phase>deploy</phase>
                    <goals>
                        <goal>github</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## 使用

```xml
<repositories>
    <repository>
        <id>io.github.SilenceShine</id>
        <url>https://raw.githubusercontent.com/SilenceShine/maven-repository/release</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>io.github.SilenceShine</groupId>
    <artifactId>shine-framework-core</artifactId>
    <version>0.3.1</version>
</dependency>
```
