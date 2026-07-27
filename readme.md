# Hash Pre-Image

[English](#english) · [Français](#français) · [Português](#português)

---

## English

A brute-force search for a SHA-1 collision, which is really an excuse for a better
question: **how many threads can this machine actually sustain, and how do you find that
out while the program is running?**

The work itself is deliberately dumb. [`SHA1Unit`](src/main/java/com/github/valtoni/SHA1Unit.java)
generates random bytes and hashes them; [`CollisionTask`](src/main/java/com/github/valtoni/CollisionTask.java)
keeps producing new ones until a digest matches the pre-image drawn at startup. Pure CPU,
no I/O, no waiting — exactly the profile that exposes the cost of coordinating threads.

### The four strategies

[`Collider`](src/main/java/com/github/valtoni/Collider.java) runs the same search four
ways. Uncomment the one you want in `main`:

| Method | Strategy |
|---|---|
| `SHA1ThreadedCollisionTest(n)` | Fixed pool of platform threads, sized by `availableProcessors()`. |
| `SHA1ThreadedVirtualCollisionTest(n)` | The same with `newVirtualThreadPerTaskExecutor()`. |
| `SHA1DynamicThreadedCollisionTest(load, margin)` | A platform pool that resizes itself while running. |
| `SHA1DynamicThreadedVirtualCollisionTest(load, margin)` | The same, with virtual threads. This is the one enabled by default. |

Comparing the first two against the last two is the point of the project. Virtual threads
shine under blocking workloads; here, on purely computational work, the result is less
obvious than the marketing suggests.

### The dynamic adjuster

[`DynamicThreadAdjuster`](src/main/java/com/github/valtoni/DynamicThreadAdjuster.java) runs
on its own thread as a control loop:

1. Reads process CPU load from `OperatingSystemMXBean`.
2. Compares it against the target load, within a tolerance margin.
3. Below target, submits new `CollisionTask`s. Above it, cancels the most recent ones with
   `Future.cancel(true)`.
4. Never exceeds `availableProcessors() * 4`.

The tasks cooperate with cancellation: `CollisionTask` checks
`Thread.currentThread().isInterrupted()` on every iteration and leaves the loop.

```java
SHA1DynamicThreadedVirtualCollisionTest(95, 10);  // target 95% CPU, tolerate 10%
```

### Running

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.github.valtoni.Collider
```

Requires **JDK 21**. Each task prints its PID and thread name, and reports the running
total every million attempts.

The program does not terminate. Finding a SHA-1 collision by brute force takes on the
order of 2⁸⁰ attempts — this is for watching thread behaviour, not for finding the
collision.

### Known rough edges

Listed here because they are exactly the details this project's subject makes interesting:

- `Collider.TOTAL_TESTS` is an `AtomicLong`, but it is updated with `set(get() + tests)` —
  a separate read and write, which is a race. It should be `addAndGet(tests)`. In a project
  about concurrency, that is worth fixing.
- `SHA1Unit` computes a digest of 64 zeroed bytes and then overwrites the result with
  random bytes on the next line. The first call is dead code.
- `SHA1DynamicThreadedCollisionTest` starts the adjuster and returns without awaiting
  anything.

---

## Français

Une recherche par force brute d'une collision SHA-1, qui sert surtout de prétexte à une
meilleure question : **combien de fils cette machine peut-elle réellement soutenir, et
comment le découvrir pendant que le programme tourne ?**

Le travail lui-même est volontairement bête. [`SHA1Unit`](src/main/java/com/github/valtoni/SHA1Unit.java)
génère des octets aléatoires et les hache ; [`CollisionTask`](src/main/java/com/github/valtoni/CollisionTask.java)
en produit sans relâche jusqu'à ce qu'une empreinte corresponde à la pré-image tirée au
départ. Du calcul pur, sans E/S, sans attente — exactement le profil qui expose le coût de
la coordination des fils.

### Les quatre stratégies

[`Collider`](src/main/java/com/github/valtoni/Collider.java) exécute la même recherche de
quatre façons. Décommentez celle que vous voulez dans `main` :

| Méthode | Stratégie |
|---|---|
| `SHA1ThreadedCollisionTest(n)` | Pool fixe de fils de plateforme, dimensionné par `availableProcessors()`. |
| `SHA1ThreadedVirtualCollisionTest(n)` | Le même avec `newVirtualThreadPerTaskExecutor()`. |
| `SHA1DynamicThreadedCollisionTest(charge, marge)` | Un pool de plateforme qui se redimensionne en cours d'exécution. |
| `SHA1DynamicThreadedVirtualCollisionTest(charge, marge)` | Le même, avec des fils virtuels. C'est celle activée par défaut. |

Comparer les deux premières aux deux dernières est tout l'intérêt du projet. Les fils
virtuels excellent sous charge bloquante ; ici, sur du calcul pur, le résultat est moins
évident que ne le laisse entendre la publicité.

### L'ajusteur dynamique

[`DynamicThreadAdjuster`](src/main/java/com/github/valtoni/DynamicThreadAdjuster.java)
tourne sur son propre fil, en boucle de régulation :

1. Lit la charge CPU du processus via `OperatingSystemMXBean`.
2. La compare à la charge cible, avec une marge de tolérance.
3. Sous la cible, soumet de nouvelles `CollisionTask`. Au-dessus, annule les plus récentes
   avec `Future.cancel(true)`.
4. Ne dépasse jamais `availableProcessors() * 4`.

Les tâches coopèrent à l'annulation : `CollisionTask` vérifie
`Thread.currentThread().isInterrupted()` à chaque itération et sort de la boucle.

```java
SHA1DynamicThreadedVirtualCollisionTest(95, 10);  // vise 95 % de CPU, tolère 10 %
```

### Exécution

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.github.valtoni.Collider
```

Nécessite le **JDK 21**. Chaque tâche affiche son PID et son nom de fil, et rapporte le
total cumulé tous les millions de tentatives.

Le programme ne se termine pas. Trouver une collision SHA-1 par force brute demande de
l'ordre de 2⁸⁰ tentatives — il s'agit d'observer le comportement des fils, pas de trouver
la collision.

### Défauts connus

Listés ici parce qu'ils touchent précisément à ce qui rend le sujet de ce projet
intéressant :

- `Collider.TOTAL_TESTS` est un `AtomicLong`, mais la mise à jour se fait par
  `set(get() + tests)` — lecture et écriture séparées, donc une course. Il faudrait
  `addAndGet(tests)`. Dans un projet sur la concurrence, cela mérite correction.
- `SHA1Unit` calcule une empreinte de 64 octets à zéro puis écrase le résultat par des
  octets aléatoires à la ligne suivante. Le premier appel est du code mort.
- `SHA1DynamicThreadedCollisionTest` démarre l'ajusteur et retourne sans rien attendre.

---

## Português

Uma busca por colisão em SHA-1 que serve de desculpa para uma pergunta melhor: **quantas
threads a máquina realmente aguenta, e como descobrir isso enquanto o programa roda?**

O trabalho em si é deliberadamente burro. [`SHA1Unit`](src/main/java/com/github/valtoni/SHA1Unit.java)
gera bytes aleatórios e calcula o hash deles; [`CollisionTask`](src/main/java/com/github/valtoni/CollisionTask.java)
fica gerando novos até bater com o digest da pré-imagem sorteada no início. É CPU pura, sem
I/O, sem espera — exatamente o perfil que expõe o custo de coordenar threads.

### As quatro estratégias

[`Collider`](src/main/java/com/github/valtoni/Collider.java) executa a mesma busca de
quatro formas. Descomente a que quiser no `main`:

| Método | Estratégia |
|---|---|
| `SHA1ThreadedCollisionTest(n)` | Pool fixo de threads de plataforma, dimensionado por `availableProcessors()`. |
| `SHA1ThreadedVirtualCollisionTest(n)` | O mesmo com `newVirtualThreadPerTaskExecutor()`. |
| `SHA1DynamicThreadedCollisionTest(carga, margem)` | Pool de plataforma que se redimensiona sozinho durante a execução. |
| `SHA1DynamicThreadedVirtualCollisionTest(carga, margem)` | O mesmo, com threads virtuais. É o que está ativo por padrão. |

Comparar as duas primeiras contra as duas últimas é o ponto do projeto. Threads virtuais
brilham em carga bloqueante; aqui, em trabalho puramente computacional, o resultado é menos
óbvio do que a propaganda sugere.

### O ajuste dinâmico

[`DynamicThreadAdjuster`](src/main/java/com/github/valtoni/DynamicThreadAdjuster.java) roda
numa thread própria e faz um laço de controle:

1. Lê a carga de CPU do processo pelo `OperatingSystemMXBean`.
2. Compara com a carga-alvo, respeitando uma margem de tolerância.
3. Abaixo do alvo, submete novas `CollisionTask`. Acima, cancela as últimas via
   `Future.cancel(true)`.
4. Nunca passa de `availableProcessors() * 4`.

As tarefas cooperam com o cancelamento: `CollisionTask` checa
`Thread.currentThread().isInterrupted()` a cada iteração e sai do laço.

```java
SHA1DynamicThreadedVirtualCollisionTest(95, 10);  // mira 95% de CPU, tolera 10%
```

### Rodando

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.github.valtoni.Collider
```

Requer **JDK 21**. Cada tarefa imprime PID e nome da thread, e a cada milhão de tentativas
reporta o total acumulado.

O programa não termina. Encontrar uma colisão de SHA-1 por força bruta exige da ordem de
2⁸⁰ tentativas — é para observar o comportamento das threads, não para achar a colisão.

### Coisas para melhorar

Ficam anotadas aqui porque são justamente os detalhes que o assunto do projeto torna
interessantes:

- `Collider.TOTAL_TESTS` é um `AtomicLong`, mas a atualização é `set(get() + tests)` —
  leitura e escrita separadas, ou seja, uma corrida. O correto é `addAndGet(tests)`. Num
  projeto sobre concorrência, vale corrigir.
- `SHA1Unit` calcula um digest de 64 bytes zerados e sobrescreve o resultado com bytes
  aleatórios na linha seguinte. A primeira chamada é código morto.
- `SHA1DynamicThreadedCollisionTest` inicia o ajustador e retorna sem aguardar nada.