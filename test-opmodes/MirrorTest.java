package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

// Sim check: all motors +0.5. Without REVERSE the left wheels spin backward;
// after setDirection(REVERSE) on the left side, all four spin forward.
@TeleOp(name = "Mirror Test")
public class MirrorTest extends LinearOpMode {
    @Override
    public void runOpMode() {
        DcMotor fl = hardwareMap.get(DcMotor.class, "frontLeft");
        DcMotor fr = hardwareMap.get(DcMotor.class, "frontRight");
        DcMotor bl = hardwareMap.get(DcMotor.class, "backLeft");
        DcMotor br = hardwareMap.get(DcMotor.class, "backRight");
        waitForStart();

        DcMotor[] all = {fl, fr, bl, br};
        for (DcMotor m : all) m.setPower(0.5);
        telemetry.addLine("Phase 1: no REVERSE");
        telemetry.update();
        sleep(1500);

        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        bl.setDirection(DcMotor.Direction.REVERSE);
        telemetry.addLine("Phase 2: left side REVERSE");
        telemetry.addData("FL enc", fl.getCurrentPosition());
        telemetry.update();
        sleep(1500);

        while (opModeIsActive()) idle();
    }
}
