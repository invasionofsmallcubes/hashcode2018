package com.invasionofsmallcubes;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.valueOf;
import static java.lang.Math.abs;

public class ReadFileTest {
    @Test
    public void readFile() throws IOException, URISyntaxException {
        Stream.of("a_example", "b_should_be_easy", "c_no_hurry", "d_metropolis", "e_high_bonus").forEach(filename -> {
            List<String> lines = null;
            try {
                lines = getLines("/"+filename+".in");

                String firstLine = lines.get(0);
                String[] splittedElements = firstLine.split(" ");
                int rowsNumber = valueOf(splittedElements[0]);
                int columnsNumber = valueOf(splittedElements[1]);
                int fleetNumber = valueOf(splittedElements[2]);
                int ridesNumber = valueOf(splittedElements[3]);
                int bonusPerRideStartingOnTime = valueOf(splittedElements[4]);
                int steps = valueOf(splittedElements[5]);

                Conf conf = new Conf(rowsNumber, columnsNumber, fleetNumber, ridesNumber, bonusPerRideStartingOnTime, steps);
                System.out.println(conf);

                List<Ride> rides = new ArrayList();

                for (int i = 1; i <= ridesNumber; i++) {
                    String[] currentRide = lines.get(i).split(" ");
                    int startingRow = valueOf(currentRide[0]);
                    int startingColumn = valueOf(currentRide[1]);
                    int endRow = valueOf(currentRide[2]);
                    int endColumn = valueOf(currentRide[3]);
                    int ts = valueOf(currentRide[4]);
                    int tf = valueOf(currentRide[5]);
                    int rideIndex = i - 1;
                    Ride r = new Ride(startingRow, startingColumn, endRow, endColumn, ts, tf, rideIndex, rideIndex);
//            System.out.println(r);
                    rides.add(r);
                }

                List<Vehicle> vehicles = new ArrayList<>();
                for (int v = 1; v <= conf.fleetNumber; v++) {
                    vehicles.add(new Vehicle(v));
                }

                for (int step = 0; step < conf.steps; step++) {
                    for (int v = 0; v < conf.fleetNumber; v++) {
                        final int currentStep = step;
                        final Vehicle vehicle = vehicles.get(v);
                        if (vehicle.isFree()) {
                            Optional<Score> max = selectNextRide(conf, rides, currentStep, vehicle);
                            max.ifPresent(s -> {
//                        System.out.println("Chosen: " + s.ride + " for vechicle:" + vehicle.v);
                                vehicle.currentRide(s.ride, s.dConnection);
                                rides.remove(s.ride);
                                vehicle.move(currentStep);
                            });
                        } else {
                            vehicle.move(currentStep);
                        }
                    }
                }

                String output = vehicles
                        .stream()
                        .map(v -> v.rides.size() + " " + v.rides.stream().map(r -> "" + r.rideIndex).collect(Collectors.joining(" ")))
                        .collect(Collectors.joining("\n"));

                Files.write(new File(filename+".out").toPath(), output.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

//        assertThat(lines.size(), is(4));
//        assertThat(rides.size(), is(0));
    }

    private Optional<Score> selectNextRide(Conf conf, List<Ride> rides, int currentStep, Vehicle vehicle) {
//        System.out.println();
        return rides.stream().map(
                r -> {
                    int dConnection = abs(r.startingRow - vehicle.row) + abs(r.startingColumn - vehicle.column);
                    Score score = new Score(0, r, dConnection);

                    boolean gettingBonusForStartingOnTime = (r.ts - currentStep) > dConnection;
                    if (gettingBonusForStartingOnTime) {
                        score.score += conf.bonusPerRideStartingOnTime;
                    }

                    int totalDistance = dConnection + r.distance;

                    if (conf.steps - currentStep > totalDistance) {
                        score.score += r.distance;
                    }

                    if (r.tf - currentStep > totalDistance) {
                        score.score += 1;
                    }

//                    System.out.println(score);
                    return score;
                }
        ).max(Comparator.comparingInt(x -> x.score));
    }

    private List<String> getLines(String inputFile) throws IOException {
        String fileToRead = getClass()
                .getResource(inputFile)
                .getFile();

        File file = new File(fileToRead);
        return FileUtils.readLines(file, "UTF-8");
    }

    private class Score {
        public int score;
        public Ride ride;
        public int dConnection;

        Score(int score, Ride ride, int dConnection) {
            this.score = score;
            this.ride = ride;
            this.dConnection = dConnection;
        }

        @Override
        public String toString() {
            return "Score{" +
                    "score=" + score +
                    ", ride=" + ride +
                    ", dConnection=" + dConnection +
                    '}';
        }
    }

    private class Ride {
        public final int startingRow;
        public final int startingColumn;
        public final int endRow;
        public final int endColumn;
        public final int ts;
        public final int tf;
        public final int rideIndex;
        public final int distance;


        public Ride(int startingRow, int startingColumn, int endRow, int endColumn, int ts, int tf, int rideIndex, int index) {
            this.startingRow = startingRow;
            this.startingColumn = startingColumn;
            this.endRow = endRow;
            this.endColumn = endColumn;
            this.ts = ts;
            this.tf = tf;
            this.rideIndex = rideIndex;
            this.distance = abs(startingRow - endRow) + abs(startingColumn - endColumn);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Ride ride = (Ride) o;

            return rideIndex == ride.rideIndex;
        }

        @Override
        public int hashCode() {
            return rideIndex;
        }

        @Override
        public String toString() {
            return "Ride{" +
                    "startingRow=" + startingRow +
                    ", startingColumn=" + startingColumn +
                    ", endRow=" + endRow +
                    ", endColumn=" + endColumn +
                    ", ts=" + ts +
                    ", tf=" + tf +
                    ", rideIndex=" + rideIndex +
                    '}';
        }
    }

    private class Conf {
        public final int rowsNumber;
        public final int columnsNumber;
        public final int fleetNumber;
        public final int ridesNumber;
        public final int bonusPerRideStartingOnTime;
        public final int steps;

        public Conf(int rowsNumber, int columnsNumber, int fleetNumber, int ridesNumber, int bonusPerRideStartingOnTime, int steps) {
            this.rowsNumber = rowsNumber;
            this.columnsNumber = columnsNumber;
            this.fleetNumber = fleetNumber;
            this.ridesNumber = ridesNumber;
            this.bonusPerRideStartingOnTime = bonusPerRideStartingOnTime;
            this.steps = steps;
        }

        @Override
        public String toString() {
            return "Conf{" +
                    "rowsNumber=" + rowsNumber +
                    ", columnsNumber=" + columnsNumber +
                    ", fleetNumber=" + fleetNumber +
                    ", ridesNumber=" + ridesNumber +
                    ", bonusPerRideStartingOnTime=" + bonusPerRideStartingOnTime +
                    ", steps=" + steps +
                    '}';
        }
    }

    private class Vehicle {
        public int row;
        public int column;

        List<Ride> rides = new ArrayList<>();
        public Ride currentRide;
        public int dConnection;
        public int rideStepRemaining;
        public int v;

        public Vehicle(int v) {
            this.v = v;
        }

        public void currentRide(Ride currentRide, int dConnection) {
            this.currentRide = currentRide;
            this.rideStepRemaining = currentRide.distance;
            this.dConnection = dConnection;
        }

        public boolean isFree() {
            return currentRide == null;
        }

        public void move(int currentStep) {
            if (dConnection > 0) {
                dConnection--;
            } else {
                if (currentStep >= currentRide.ts) {
                    rideStepRemaining--;
                }
            }

            if (rideStepRemaining == 0) {
                rides.add(currentRide);
                currentRide = null;
            }

        }

        public void addRide(Ride ride) {
            rides.add(ride);
        }
    }
}
