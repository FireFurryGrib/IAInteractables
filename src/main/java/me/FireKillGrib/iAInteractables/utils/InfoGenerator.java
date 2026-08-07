package me.FireKillGrib.iAInteractables.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class InfoGenerator {

    public static void generate(File dataFolder) {
        File infoFile = new File(dataFolder, "info.yml");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(infoFile, false))) {
            writer.println("# ========================================================================");
            writer.println("# IAInteractables - Information & Documentation File");
            writer.println("# ========================================================================");
            writer.println("# EN: This file is auto-generated on every server start. Do not edit it!");
            writer.println("# RU: Этот файл автоматически генерируется при каждом запуске. Не редактируйте его!");
            writer.println("# ========================================================================\n");

            writer.println("# ------------------------------------------------------------------------");
            writer.println("# [1] GUI STRUCTURE / СТРУКТУРА МЕНЮ");
            writer.println("# ------------------------------------------------------------------------");
            writer.println("# EN: 'structure' defines the layout. Put '$' for special functions.");
            writer.println("# RU: 'structure' задает вид меню. Используйте '$' для спец. слотов.");
            writer.println("#     You must define tags in 'special_functions' list to map to '$'.");
            writer.println("#     Вы должны указать теги в списке 'special_functions' для привязки к '$'.\n");
            writer.println("# EXAMPLE / ПРИМЕР (default.yml):");
            writer.println("# structure:");
            writer.println("#   - \"X A X B X\"");
            writer.println("#   - \"X $ X $ X\"");
            writer.println("# special_functions: [PRO, RES]");
            writer.println("# -> The first $ becomes PRO (Progress bar), the second becomes RES (Result).");
            writer.println("");

            writer.println("# ------------------------------------------------------------------------");
            writer.println("# [2] SPECIAL FUNCTIONS / СПЕЦ. ФУНКЦИИ ДЛЯ СЕТКИ (special_functions)");
            writer.println("# ------------------------------------------------------------------------");
            writer.println("#   PRO - Progress bar (Индикатор прогресса)");
            writer.println("#   RES - Result slot (Слот результата)");
            writer.println("#   BAC - Back button (Кнопка 'Назад')");
            writer.println("#   BUC - Bucket slot (Слот для вёдер: заливка жидкостей)");
            writer.println("#   LQ1, LQ2, LQ3 - Fluid tank display (Отображение конкретной жидкости в резервуаре)");
            writer.println("#   LQU - Universal fluid display (Отображение преобладающей жидкости)\n");

            writer.println("# ------------------------------------------------------------------------");
            writer.println("# [3] SPECIAL TAGS / СПЕЦ. ТЕГИ МЕХАНИЗМОВ (special_tags)");
            writer.println("# ------------------------------------------------------------------------");
            writer.println("# EN: Add these to the ROOT of your machine config, OR inside a specific recipe.");
            writer.println("# RU: Добавляйте их в КОРЕНЬ конфига механизма, ИЛИ внутрь конкретного рецепта.\n");
            
            writer.println("# > PUM (Put in root / Писать в корне конфига печки):");
            writer.println("#   EN: Pump mode. Automatically pumps nearby fluids if conditions are met.");
            writer.println("#   RU: Режим помпы. Качает жидкость, требуя топливо.");
            writer.println("#   EXAMPLE:");
            writer.println("#   special_tags:");
            writer.println("#     PUM:");
            writer.println("#       target_fluid: \"minecraft:water\"");
            writer.println("#       target_block: \"WATER\" # Can be IA block like 'ia-my_oil'");
            writer.println("#       required_blocks: 100 # How many blocks needed nearby / Размер водоема");
            writer.println("#       amount_per_cycle: 81 # Amount in LN per cycle (81 = 1 Bucket) / Объем за 1 раз");
            writer.println("#       cycle_ticks: 100 # Time between cycles / Время цикла");
            writer.println("#       fuel_slot: \"U\" # Slot character to consume fuel from / Слот с топливом");
            writer.println("#       fuel_amount: 1 # Consumed amount per cycle / Потребление топлива\n");

            writer.println("# > COL (Put inside recipe / Писать внутри рецепта):");
            writer.println("#   EN: Cooling requirement. Explodes if no coolant.");
            writer.println("#   RU: Требование охлаждения. Взорвется без охладителя.");
            writer.println("#   EXAMPLE:");
            writer.println("#   recipes:");
            writer.println("#     1:");
            writer.println("#       special_tags:");
            writer.println("#         COL:");
            writer.println("#           amount_per_recipe: 27 # Total LN consumed / Потребление ЖС");
            writer.println("#           explode_timer: 200 # Ticks before boom / Тиков до взрыва");
            writer.println("#           coolants: [water_based, cold] # Allowed groups / Разрешенные группы\n");

            writer.println("# ------------------------------------------------------------------------");
            writer.println("# [4] FLUID PHYSICS / ФИЗИКА ЖИДКОСТЕЙ");
            writer.println("# ------------------------------------------------------------------------");
            writer.println("# EN: Internal unit is LN (Liquid Nugget). 1 Bucket = 1000 mB = 81 LN.");
            writer.println("# RU: Внутренняя единица измерения - ЖС (Жидкостный самородок). 1 Ведро = 1000 mB = 81 ЖС.");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}