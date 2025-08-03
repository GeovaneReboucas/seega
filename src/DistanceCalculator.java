// package com.locationchat.util;

// import model.Location;

public class DistanceCalculator {
    
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calcula a distância entre duas localizações usando a fórmula de Haversine
     * @param location1 Primeira localização
     * @param location2 Segunda localização
     * @return Distância em quilômetros
     */
    public static double calculateDistance(Location location1, Location location2) {
        double lat1Rad = Math.toRadians(location1.getLatitude());
        double lat2Rad = Math.toRadians(location2.getLatitude());
        double deltaLatRad = Math.toRadians(location2.getLatitude() - location1.getLatitude());
        double deltaLonRad = Math.toRadians(location2.getLongitude() - location1.getLongitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Verifica se dois usuários estão dentro do raio de comunicação
     * @param user1 Primeiro usuário
     * @param user2 Segundo usuário
     * @param radius Raio de comunicação em quilômetros
     * @return true se estão dentro do raio, false caso contrário
     */
    public static boolean isWithinRadius(Location location1, Location location2, double radius) {
        return calculateDistance(location1, location2) <= radius;
    }
}

