# FTC OpMode Sim

A Processing library that lets students run real FTC SDK OpMode code and see a 2D simulation of their robot, before they ever touch real hardware.

Students write the same Java they would put on a real robot. The sim shows a top-down mecanum chassis with spinning wheels, a direction indicator, and a Driver Station panel with INIT, PLAY, STOP and telemetry.

## Status

Early prototype. Planning and first demo in progress.

## Features (v1)

- Real FTC SDK code: `LinearOpMode`, `hardwareMap`, `DcMotor`, `telemetry`, `ElapsedTime`
- Top-down 2D mecanum robot (frontLeft, frontRight, backLeft, backRight)
- Wheel spin arrows and a center direction indicator
- Real motor mirroring, so students learn to set direction and power
- Simulated encoder counts
- Driver Station panel with INIT / PLAY / STOP and telemetry
- Real Driver Station errors for wrong hardware names

## Requirements

- Processing 4

## How it works

1. Open the provided sketch in Processing.
2. Add a new `.java` tab with your OpMode.
3. Press Run.

## Roadmap

- Gamepad support
- Multiple OpModes
- Servos and sensors
- p5.js web version

## About

Built by NYC FIRST for robotics education.
