/*
Instructions:

1. Edit this OpMode, then press Build Everything.
2. On the Driver Station, press INIT, then PLAY.
3. On a real robot: create a new linear op mode named FourMotorTest.java and copy paste this code.

*/


package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name = "4 Motor Test Routine", group = "Testing")
public class FourMotorTest extends LinearOpMode {

    private final String[] motorNames = {
        "frontLeft",   // Port 0
        "frontRight",  // Port 1
        "backLeft",    // Port 2
        "backRight"    // Port 3
    };

    private final DcMotor[] motors = new DcMotor[4];

    private static final double TEST_POWER = 0.25;
    private static final double TEST_TIME = 5.0;
    private static final double PAUSE_TIME = 3.0;

    @Override
    public void runOpMode() {

        // Initialize motors
        for (int i = 0; i < motors.length; i++) {
            motors[i] = hardwareMap.get(
                DcMotor.class, motorNames[i]);

            motors[i].setPower(0);
            motors[i].setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motors[i].setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // Show instructions before PLAY
        telemetry.addLine("FTC MOTOR DIAGNOSTIC");
        telemetry.addLine("====================");
        telemetry.addLine("EXPECTED CONFIGURATION:");
        telemetry.addLine("Port 0: frontLeft");
        telemetry.addLine("Port 1: frontRight");
        telemetry.addLine("Port 2: backLeft");
        telemetry.addLine("Port 3: backRight");
        telemetry.addLine();
        telemetry.addLine("ROBOT LAYOUT (TOP VIEW)");
        telemetry.addLine("       FRONT");
        telemetry.addLine("         ^");
        telemetry.addLine("  FL[0]     [1]FR");
        telemetry.addLine("     +-------+");
        telemetry.addLine("     |          |");
        telemetry.addLine("     |          |");
        telemetry.addLine("     +-------+");
        telemetry.addLine("  BL[2]     [3]BR");
        telemetry.addLine("        BACK");
        telemetry.addLine();
        telemetry.addLine("INSTRUCTIONS:");
        telemetry.addLine("1. Raise wheels off ground.");
        telemetry.addLine("2. Verify motor names/ports.");
        telemetry.addLine("3. Face robot front away.");
        telemetry.addLine("4. Press PLAY to begin.");
        telemetry.addLine();
        telemetry.addData("Test Power", "25%");
        telemetry.addData("Motor Duration", "5 seconds");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) return;

        ElapsedTime timer = new ElapsedTime();

        try {
            for (int i = 0;
                 i < motors.length && opModeIsActive();
                 i++) {

                stopAllMotors();

                DcMotor motor = motors[i];
                motor.setPower(TEST_POWER);
                timer.reset();

                while (opModeIsActive()
                        && timer.seconds() < TEST_TIME) {

                    telemetry.clearAll();
                    telemetry.addLine("MOTOR TEST IN PROGRESS");
                    telemetry.addLine("====================");
                    telemetry.addData("Test", "%d of 4", i + 1);
                    telemetry.addData("Motor", motorNames[i]);
                    telemetry.addData("Expected Port", i);
                    telemetry.addData("Power", "%.0f%%",
                            TEST_POWER * 100);
                    telemetry.addData("Time Remaining", "%.1f sec",
                            Math.max(0,
                                TEST_TIME - timer.seconds()));
                    telemetry.addData("Encoder",
                            motor.getCurrentPosition());
                    telemetry.addLine();
                    telemetry.addLine("CHECK:");
                    telemetry.addLine(
                        "Is the correct wheel spinning?");
                    telemetry.addLine(
                        "Is only ONE wheel spinning?");
                    telemetry.update();

                    idle();
                }

                motor.setPower(0);

                
                // Pause between motors
                timer.reset();

                while (opModeIsActive()
                        && timer.seconds() < PAUSE_TIME) {

                    double remaining = Math.max(
                            0, PAUSE_TIME - timer.seconds());

                    telemetry.clearAll();
                    telemetry.addLine("MOTOR TEST - PAUSED");
                    telemetry.addLine("====================");
                    telemetry.addData("Completed",
                            motorNames[i]);
                    telemetry.addData("Next",
                            i < 3 ? motorNames[i + 1]
                                  : "Test Complete");
                    telemetry.addData("Starting In",
                            "%.1f seconds", remaining);
                    telemetry.addLine();
                    telemetry.addLine(
                            "Keep hands clear of wheels!");
                    telemetry.update();

                    idle();
                }

            }
        } finally {
            stopAllMotors();
        }

        if (opModeIsActive()) {
            telemetry.clearAll();
            telemetry.addLine("MOTOR TEST COMPLETE!");
            telemetry.addLine("====================");
            telemetry.addLine("All four motors tested.");
            telemetry.addLine();
            telemetry.addLine("Verify:");
            telemetry.addLine("- Correct motor locations");
            telemetry.addLine("- Only one motor at a time");
            telemetry.addLine("- All motors responded");
            telemetry.addLine();
            telemetry.addLine("Press STOP to exit.");
            telemetry.update();

            while (opModeIsActive()) {
                idle();
            }
        }
    }

    private void stopAllMotors() {
        for (DcMotor motor : motors) {
            if (motor != null) {
                motor.setPower(0);
            }
        }
    }
}
