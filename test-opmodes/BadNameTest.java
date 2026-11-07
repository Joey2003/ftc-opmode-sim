package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

// Sim check: misspelled hardware name -> real SDK error, OpMode stops.
@TeleOp(name = "Bad Name Test")
public class BadNameTest extends LinearOpMode {
    @Override
    public void runOpMode() {
        DcMotor m = hardwareMap.get(DcMotor.class, "frontLeftt");
        waitForStart();
    }
}
