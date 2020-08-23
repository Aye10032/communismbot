package com.aye10032.Functions;

import com.aye10032.Functions.funcutil.BaseFunc;
import com.aye10032.Functions.funcutil.SimpleMsg;
import com.aye10032.Utils.ConfigLoader;
import com.aye10032.Utils.FoodUtil;
import com.aye10032.Utils.food.FoodClaass;
import com.aye10032.Zibenbot;

/**
 * @author Dazo66
 */
public class EatFunc extends BaseFunc {

    FoodUtil foodUtil;
    FoodClaass foodClaass = ConfigLoader.load(zibenbot.appDirectory + "/foodData.json", FoodClaass.class);

    public EatFunc(Zibenbot zibenbot) {
        super(zibenbot);
    }

    @Override
    public void setUp() {
        this.foodUtil = new FoodUtil();
    }

    public void run(SimpleMsg CQmsg) {
        if (CQmsg.getMsg().equals("晚饭")) {
            if (CQmsg.getFromGroup() == 792666782L) {
                if (foodClaass.getTimes(CQmsg.getFromClient()) == 99) {
                    foodClaass.resetTimes(CQmsg.getFromClient());
                    String[] food = foodUtil.eatGuaranteed(3);
                    zibenbot.replyMsg(CQmsg, food[0]);
                } else {
                    String[] food = foodUtil.eatWhatWithSSR();
                    zibenbot.replyMsg(CQmsg, food[0]);
                    if (food[1].equals("3")) {
                        foodClaass.resetTimes(CQmsg.getFromClient());
                    } else {
                        foodClaass.addOne(CQmsg.getFromClient());
                    }
                }
            } else {
                String food = foodUtil.eatWhat();
                zibenbot.replyMsg(CQmsg, food);
            }
            ConfigLoader.save(zibenbot.appDirectory + "/foodData.json", FoodClaass.class, foodClaass);
        } else if (CQmsg.getFromGroup() == 792666782L && CQmsg.getMsg().equals("晚饭十连")) {
            StringBuilder foodBuilder = new StringBuilder();
            boolean hasSSR = false;
            for (int i = 0; i < 9; i++) {
                if (foodClaass.getTimes(CQmsg.getFromClient()) == 99) {
                    foodClaass.resetTimes(CQmsg.getFromClient());
                    String[] food = foodUtil.eatGuaranteed(3);
                    zibenbot.replyMsg(CQmsg, food[0]);
                } else {
                    String[] food = foodUtil.eatWhatWithSSR();
                    foodBuilder.append(food[0]).append("\n");
                    switch (food[1]) {
                        case "1":
                            foodClaass.addOne(CQmsg.getFromClient());
                            break;
                        case "2":
                            hasSSR = true;
                            foodClaass.addOne(CQmsg.getFromClient());
                            break;
                        case "3":
                            hasSSR = true;
                            foodClaass.resetTimes(CQmsg.getFromClient());
                            break;
                    }
                }
            }

            if (foodClaass.getTimes(CQmsg.getFromClient()) == 99) {
                foodClaass.resetTimes(CQmsg.getFromClient());
                String[] food = foodUtil.eatGuaranteed(3);
                foodBuilder.append(food[0]);
            } else {
                if (!hasSSR) {
                    String[] food = foodUtil.eatGuaranteed(2);
                    foodBuilder.append(food[0]);

                    if (food[1].equals("3")) {
                        foodClaass.resetTimes(CQmsg.getFromClient());
                    } else {
                        foodClaass.addOne(CQmsg.getFromClient());
                    }

                } else {
                    String[] food = foodUtil.eatWhatWithSSR();
                    foodBuilder.append(food[0]);
                    switch (food[1]) {
                        case "1":
                            foodClaass.addOne(CQmsg.getFromClient());
                            break;
                        case "2":
                            hasSSR = true;
                            foodClaass.addOne(CQmsg.getFromClient());
                            break;
                        case "3":
                            hasSSR = true;
                            foodClaass.resetTimes(CQmsg.getFromClient());
                            break;
                    }
                }
            }
            zibenbot.replyMsg(CQmsg, foodBuilder.toString());
            ConfigLoader.save(zibenbot.appDirectory + "/foodData.json", FoodClaass.class, foodClaass);
        }
    }
}