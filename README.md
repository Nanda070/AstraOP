# AstraOR

Плагин для **Paper 1.21.1** с системой кастомных классов в стиле Origins для Minecraft.

## Что это за проект

**AstraOR** добавляет 12 игровых классов с пассивными эффектами, ограничениями и активными способностями на настраиваемый keybind.  
Класс игрока и служебное состояние сохраняются в `PersistentDataContainer`.

## Техническая информация

| Параметр | Значение |
|---|---|
| Название плагина | `AstraOR` |
| Версия плагина (`plugin.yml`) | `1.0.0` |
| `api-version` | `1.21` |
| Главный класс | `com.astraor.AstraORPlugin` |
| Maven `groupId` | `com.astraor` |
| Maven `artifactId` | `AstraOR` |
| Maven версия | `1.0.0-SNAPSHOT` |
| Java | `21` |
| Авторы | `AstraTeam` |
| `depend` | `AuthMe`, `AstraSMP` |

## Зависимости

### Обязательные зависимости сервера

- **Paper API** `1.21.1-R0.1-SNAPSHOT`
- **AuthMe** `5.6.0-SNAPSHOT`
- **AstraSMP** `1.0-SNAPSHOT`

> Важно: `AuthMe` и `AstraSMP` должны быть установлены на сервере.  
> `AstraSMP` в этом проекте подключён как локальная `system/compile`-зависимость из `libs/AstraSMP.jar`.

## Сборка

| Действие | Команда |
|---|---|
| Проверить компиляцию | `mvn clean compile` |
| Собрать артефакт | `mvn clean package` |

## Что происходит при запуске плагина

При `onEnable()` инициализируются:

- `Settings`
- `ClassManager`
- `KeybindManager`
- `CooldownManager`
- `LocationManager`
- `MechanicsManager`
- `CatalystManager`
- `BossManager`

Регистрируются:

- `AuthListener`
- `FreezeListener`
- `OnboardingGUI`
- `GuideGUI`
- `NPCInteractListener`
- `MechanicsListener`
- команды (`/astraor`, `/abilitykeybind`)

## Структура исходников

Корневые пакеты `src/main/java/com/astraor`:

- `commands`
- `config`
- `gui`
- `listeners`
- `mechanics`
- `state`
- `util`

## Классы и механики

`AstraClass` содержит 12 классов:

- `ARCHITECT`
- `VANGUARD`
- `JUGGERNAUT`
- `SHADOW`
- `RANGER`
- `BERSERKER`
- `WINDWALKER`
- `PHANTOM`
- `PHOENIX`
- `DRUID`
- `VAMPIRE`
- `GOLIATH`

### Подробности по каждому классу

| Класс | Пассивные эффекты | Ограничения и уязвимости | Активная способность |
|---|---|---|---|
| **ARCHITECT** | Haste II, Speed I, иммунитет к падению, дальность взаимодействия с блоками `+2` | урон в ближнем бою `-30%`, урон от взрывов `+50%` | — |
| **VANGUARD** | ближний урон `+20%`, урон стрелами `-50%`, случайное уменьшение потери голода | — | **Adrenaline** при критически низком HP: Strength I + Speed II на 5 сек, КД 30 сек |
| **JUGGERNAUT** | `+10` max HP, `+4` armor, `+2` armor toughness, полный knockback resistance | Slowness I, запрет элитр/скольжения | — |
| **SHADOW** | Speed II, невидимость при приседании в темноте, бонус урона из невидимости | слабость под прямым дневным светом, max HP `-3` сердца | — |
| **RANGER** | урон стрел `+50%`, скорость стрел `+20%`, 30% шанс не потратить боеприпас | запрет щитов, сниженный урон в ближнем бою | — |
| **BERSERKER** | усиление урона по бронированным целям, баффы после убийства (Strength+Speed), игнор части брони | броня быстрее теряет прочность, запрет луков/арбалетов/трезубцев | — |
| **WINDWALKER** | встроенная неразрушаемая элитра, иммунитет к падению и столкновению со стеной | слабость + Mining Fatigue ниже Y=50, повышенный урон от стрел, запрет тяжёлой diamond/netherite брони | **Updraft** (полёт/рывок вверх), КД 45 сек |
| **PHANTOM** | невидимость при приседании | запрет железной/золотой/алмазной/незеритовой брони, max HP `-2` сердца | **Phase Dash** (фазовый рывок на 2 блока), КД 20 сек |
| **PHOENIX** | иммунитет к огню и лаве | урон в воде/дожде, max HP `-2` сердца | **Supernova** (область вокруг игрока: 10 урона + поджог), КД 60 сек |
| **DRUID** | безопасное употребление ряда сырых/опасных продуктов, дружелюбность spider/bee/wolf/polar bear | запрет тяжёлой брони | **Nature’s Grasp** (сильное замедление ближайших существ), КД 45 сек |
| **VAMPIRE** | lifesteal 20% от нанесённого урона | повышенный урон от огня/лавы | **Blood Dash** (затрата HP, рывок, урон и Wither по близким целям), КД 30 сек |
| **GOLIATH** | `+10` max HP, полный knockback resistance, Haste II + Slowness I | периодическая потеря голода | **Tremor** (подбрасывает и замедляет ближайших существ), КД 35 сек |

## Выбор класса (Onboarding)

После успешного входа/регистрации в **AuthMe**:

- если у игрока нет класса — открывается `OnboardingGUI` для выбора;
- во время выбора `FreezeListener` блокирует:
  - перемещение,
  - получение урона,
  - выброс предметов,
  - взаимодействие с посторонними инвентарями.

Выбранный класс и состояние выбора сохраняются через `ClassManager` в `PersistentDataContainer`.

## Keybind способностей

Поддерживаемые варианты (`KeybindManager`):

- `SHIFT_RIGHT_CLICK` (по умолчанию)
- `SHIFT_LEFT_CLICK`
- `SWAP_HANDS` (клавиша `F`)
- `DROP_ITEM` (клавиша `Q`)

Команда:

- `/abilitykeybind` — показать текущую настройку
- `/abilitykeybind set <bind>` — сменить bind

## Кулдауны способностей

| Способность | Кулдаун |
|---|---|
| Vanguard — Adrenaline | 30 сек |
| Windwalker — Updraft | 45 сек |
| Phantom — Phase Dash | 20 сек |
| Phoenix — Supernova | 60 сек |
| Druid — Nature’s Grasp | 45 сек |
| Vampire — Blood Dash | 30 сек |
| Goliath — Tremor | 35 сек |

## GuideGUI (путеводитель)

`GuideGUI` даёт:

- lore/вводную информацию о мире;
- телепорт к `money NPC`;
- телепорт к `item NPC`;
- телепорт в логово босса.

## NPC и админ-механики

Основная админ-команда: `/astraor`  
Ключевая механика спавна NPC: `/astraor spawnnpc <money|item|boss|guide>`

Дополнительно реализованы подкоманды для управления точками/сбросом/установкой классов и выдачей катализатора.

## Связанные репозитории

- [AstraBuild](https://github.com/Nanda070/AstraBuild)
- [AstraSMP](https://github.com/Nanda070/AstraSMP)
- [AstraIP](https://github.com/Nanda070/AstraIP)
